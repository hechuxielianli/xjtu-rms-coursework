package com.example.rms.change.application;
import com.example.rms.change.application.port.ChangeStore;
import com.example.rms.requirement.application.contract.*;
import com.example.rms.version.application.contract.*;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ChangeTransactions {
    public record DecisionResult(ChangeReviewData review,ChangeData change) {}
    public record ApplyResult(ChangeData change,RequirementView requirement,VersionData version) {}
    private static final Set<String> EDIT=Set.of("requestTitle","reason","proposedTitle","proposedDescription","proposedLevel","proposedKind","proposedPriority","proposedSource","proposedRationale","proposedAcceptanceCriteria","expectedLockVersion");
    private final ChangeStore store;private final RequirementMutationService requirements;private final VersionReadService versions;private final VersionAppendService append;private final AuditWriteService audit;private final Clock clock;
    public ChangeTransactions(ChangeStore store,RequirementMutationService requirements,VersionReadService versions,VersionAppendService append,AuditWriteService audit,Clock clock) { this.store=store;this.requirements=requirements;this.versions=versions;this.append=append;this.audit=audit;this.clock=clock; }
    @Transactional(rollbackFor=Exception.class)
    public ChangeData create(Actor actor,long id,String title,String reason,String expected) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);InputPolicy.nonBlank(title,200);InputPolicy.nonBlank(reason,Integer.MAX_VALUE);long revision=InputPolicy.lockVersion(expected);var parent=requirements.lockFormalContext(id);InputPolicy.expected(revision,parent.lockVersion());if(store.active(id))throw new RmsException(ErrorCode.ACTIVE_CHANGE_EXISTS);var version=versions.requireWithin(id,parent.currentVersionId());var after=store.create(id,version.versionId(),title,reason,version.content(),actor.userId(),now());success(actor,"CHANGE_CREATE",null,after);return after; }
    @Transactional(rollbackFor=Exception.class)
    public ChangeData edit(Actor actor,long id,Map<String,Object> fields) {
        actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);
        var patch=new InputPatch(fields,EDIT,"expectedLockVersion");
        long revision=InputPolicy.lockVersion(patch.string("expectedLockVersion",false));
        parent(id);
        var before=store.lock(id);
        InputPolicy.expected(revision,before.lockVersion());
        state(before,"DRAFT");

        var proposed=proposedPatch(patch,before.proposed());
        validateDraft(proposed);
        String title=patch.has("requestTitle")?patch.string("requestTitle",false):before.requestTitle();
        String reason=patch.has("reason")?patch.string("reason",false):before.reason();
        InputPolicy.nonBlank(title,200);
        InputPolicy.nonBlank(reason,Integer.MAX_VALUE);

        var after=store.edit(id,title,reason,proposed,now());
        success(actor,"CHANGE_EDIT",before,after);
        return after;
    }
    @Transactional(rollbackFor=Exception.class)
    public ChangeReviewData submit(Actor actor,long id,String expected) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);long revision=InputPolicy.lockVersion(expected);parent(id);var before=store.lock(id);InputPolicy.expected(revision,before.lockVersion());state(before,"DRAFT");before.proposed().requireComplete();InputPolicy.nonBlank(before.requestTitle(),200);InputPolicy.nonBlank(before.reason(),Integer.MAX_VALUE);versions.requireWithin(before.requirementId(),before.baseVersionId());if(store.pending(id))throw new RmsException(ErrorCode.ACTIVE_REVIEW_EXISTS);Instant now=now();var review=store.createReview(before,store.nextRound(id),actor.userId(),now);var after=store.submit(id,now);var values=reviewSafe(review);values.put("status",after.status());values.put("lockVersion",after.lockVersion());audit.success(actor,new AuditChange(before.requirementId(),"CHANGE_REVIEW",Long.toString(review.changeReviewId()),"CHANGE_SUBMIT",null,values));return review; }
    @Transactional(rollbackFor=Exception.class)
    public ChangeData cancel(Actor actor,long id,String expected) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);long revision=InputPolicy.lockVersion(expected);parent(id);var before=store.lock(id);if(before.createdBy()!=actor.userId())throw new RmsException(ErrorCode.NOT_AUTHOR);InputPolicy.expected(revision,before.lockVersion());state(before,"DRAFT");var after=store.cancel(id,now());success(actor,"CHANGE_CANCEL",before,after);return after; }
    @Transactional(rollbackFor=Exception.class)
    public DecisionResult decide(Actor actor,long reviewId,String decision,String comment,String expected) {
        actor.requireAny(RoleCode.REVIEWER);
        InputPolicy.oneOf(decision,"APPROVE","REJECT","REQUEST_CHANGES");
        long revision=InputPolicy.lockVersion(expected);
        if(!decision.equals("APPROVE"))InputPolicy.nonBlank(comment,Integer.MAX_VALUE);
        long id=store.reviewChangeId(reviewId);
        var parent=parent(id);
        var change=store.lock(id);
        if(change.createdBy()==actor.userId())throw new RmsException(ErrorCode.SELF_REVIEW);
        var before=store.lockReview(reviewId);
        if(before.requirementId()!=parent.requirementId() || before.changeRequestId()!=id || !snapshotMatches(before,change))throw new RmsException(ErrorCode.STATE_CONFLICT);
        InputPolicy.expected(revision,change.lockVersion());
        state(change,"UNDER_REVIEW");
        if(!before.reviewStatus().equals("PENDING"))throw new RmsException(ErrorCode.STATE_CONFLICT);

        Instant now=now();
        var after=store.completeReview(reviewId,decision,comment,actor.userId(),now);
        var updated=store.afterDecision(id,decision,now);
        var previous=reviewDecisionValues(before,change);
        var values=reviewDecisionValues(after,updated);
        audit.success(actor,new AuditChange(change.requirementId(),"CHANGE_REVIEW",Long.toString(reviewId),"CHANGE_REVIEW_DECISION",previous,values));
        return new DecisionResult(after,updated);
    }
    @Transactional(rollbackFor=Exception.class)
    public ApplyResult apply(Actor actor,long id,String expectedRequirement,String expectedChange) {
        actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);
        long parentRevision=InputPolicy.lockVersion(expectedRequirement);
        long revision=InputPolicy.lockVersion(expectedChange);

        // One transaction and the original lock order cover the complete aggregate operation.
        var parent=parent(id);
        var before=store.lock(id);
        InputPolicy.expected(parentRevision,parent.lockVersion());
        InputPolicy.expected(revision,before.lockVersion());
        state(before,"APPROVED");
        if(before.baseVersionId()!=parent.currentVersionId())throw new RmsException(ErrorCode.BASE_VERSION_STALE);
        before.proposed().requireComplete();
        var approved=store.latestReview(id);
        requireApprovedSnapshot(approved,before);
        versions.requireWithin(parent.requirementId(),before.baseVersionId());

        // Append, synchronize parent, complete change, then SUCCESS audit all join this transaction.
        Instant now=now();
        var version=append.appendApplied(parent.requirementId(),id,parent.currentVersionId(),before.proposed(),actor.userId(),now,before.reason());
        requirements.installAppliedVersion(parent.requirementId(),parent.currentVersionId(),version.versionId(),before.proposed(),now);
        var after=store.applied(id,actor.userId(),now);
        var requirement=requirements.readView(parent.requirementId());

        var previous=safe(before);
        previous.put("requirementStatus",parent.status());
        previous.put("currentVersionId",parent.currentVersionId());
        previous.put("requirementLockVersion",parent.lockVersion());
        var values=safe(after);
        values.put("requirementStatus",requirement.status());
        values.put("currentVersionId",requirement.currentVersionId());
        values.put("requirementLockVersion",requirement.lockVersion());
        values.put("versionId",version.versionId());
        values.put("versionNo",version.versionNo());
        values.put("changeReviewId",approved.changeReviewId());
        audit.success(actor,new AuditChange(parent.requirementId(),"CHANGE_REQUEST",Long.toString(id),"CHANGE_APPLY",previous,values));
        return new ApplyResult(after,requirement,version);
    }
    private static ContentSnapshot proposedPatch(InputPatch p,ContentSnapshot current) {
        return new ContentSnapshot(
            p.has("proposedTitle")?p.string("proposedTitle",false):current.title(),
            p.has("proposedDescription")?p.string("proposedDescription",false):current.description(),
            p.has("proposedLevel")?p.string("proposedLevel",false):current.level(),
            p.has("proposedKind")?p.string("proposedKind",false):current.kind(),
            p.has("proposedPriority")?p.string("proposedPriority",false):current.priority(),
            p.has("proposedSource")?p.string("proposedSource",true):current.source(),
            p.has("proposedRationale")?p.string("proposedRationale",true):current.rationale(),
            p.has("proposedAcceptanceCriteria")?p.string("proposedAcceptanceCriteria",true):current.acceptanceCriteria());
    }
    private static void requireApprovedSnapshot(ChangeReviewData approved,ChangeData change) {
        if(!"COMPLETED".equals(approved.reviewStatus()) || !"APPROVE".equals(approved.decision()) || approved.reviewerId()==null || approved.reviewerId()==change.createdBy() || !snapshotMatches(approved,change))throw new RmsException(ErrorCode.STATE_CONFLICT);
    }
    private static Map<String,Object> reviewDecisionValues(ChangeReviewData review,ChangeData change) {
        var values=reviewSafe(review);
        values.put("status",change.status());
        values.put("lockVersion",change.lockVersion());
        return values;
    }
    private static boolean snapshotMatches(ChangeReviewData review,ChangeData change) { return review.requirementId()==change.requirementId() && review.changeRequestId()==change.changeRequestId() && review.snapshotBaseVersionId()==change.baseVersionId() && Objects.equals(review.snapshotRequestTitle(),change.requestTitle()) && Objects.equals(review.snapshotReason(),change.reason()) && review.proposed().equals(change.proposed()); }
    private FormalRequirement parent(long changeId) { return requirements.lockFormalContext(store.requirementId(changeId)); }
    private static void state(ChangeData c,String expected) { if(!expected.equals(c.status()))throw new RmsException(ErrorCode.STATE_CONFLICT); }
    private static void validateDraft(ContentSnapshot c) { InputPolicy.nonBlank(c.title(),200);InputPolicy.nonBlank(c.description(),Integer.MAX_VALUE);InputPolicy.oneOf(c.level(),"BUSINESS","USER","SYSTEM");InputPolicy.oneOf(c.kind(),"FUNCTIONAL","QUALITY","CONSTRAINT");InputPolicy.oneOf(c.priority(),"LOW","MEDIUM","HIGH","CRITICAL"); }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private void success(Actor actor,String action,ChangeData before,ChangeData after) { audit.success(actor,new AuditChange(after.requirementId(),"CHANGE_REQUEST",Long.toString(after.changeRequestId()),action,before==null?null:safe(before),safe(after))); }
    private static Map<String,Object> safe(ChangeData r) { var c=r.proposed();var m=new LinkedHashMap<String,Object>();m.put("changeRequestId",r.changeRequestId());m.put("requirementId",r.requirementId());m.put("baseVersionId",r.baseVersionId());m.put("requestTitle",r.requestTitle());m.put("reason",r.reason());m.put("proposedTitle",c.title());m.put("proposedDescription",c.description());m.put("proposedLevel",c.level());m.put("proposedKind",c.kind());m.put("proposedPriority",c.priority());m.put("proposedSource",c.source());m.put("proposedRationale",c.rationale());m.put("proposedAcceptanceCriteria",c.acceptanceCriteria());m.put("status",r.status());m.put("createdBy",r.createdBy());m.put("createdAt",r.createdAt().toString());m.put("updatedAt",r.updatedAt().toString());m.put("appliedBy",r.appliedBy());m.put("appliedAt",r.appliedAt()==null?null:r.appliedAt().toString());m.put("lockVersion",r.lockVersion());return m; }
    private static Map<String,Object> reviewSafe(ChangeReviewData r) { var c=r.proposed();var m=new LinkedHashMap<String,Object>();m.put("changeReviewId",r.changeReviewId());m.put("requirementId",r.requirementId());m.put("changeRequestId",r.changeRequestId());m.put("roundNo",r.roundNo());m.put("snapshotBaseVersionId",r.snapshotBaseVersionId());m.put("snapshotRequestTitle",r.snapshotRequestTitle());m.put("snapshotReason",r.snapshotReason());m.put("snapshotProposedTitle",c.title());m.put("snapshotProposedDescription",c.description());m.put("snapshotProposedLevel",c.level());m.put("snapshotProposedKind",c.kind());m.put("snapshotProposedPriority",c.priority());m.put("snapshotProposedSource",c.source());m.put("snapshotProposedRationale",c.rationale());m.put("snapshotProposedAcceptanceCriteria",c.acceptanceCriteria());m.put("submittedBy",r.submittedBy());m.put("submittedAt",r.submittedAt().toString());m.put("reviewStatus",r.reviewStatus());m.put("reviewerId",r.reviewerId());m.put("decision",r.decision());m.put("comment",r.comment());m.put("decidedAt",r.decidedAt()==null?null:r.decidedAt().toString());return m; }
}

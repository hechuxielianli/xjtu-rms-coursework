package com.example.rms.review.application;
import com.example.rms.review.application.port.ReviewStore;
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
public class ReviewTransactions {
    public record DecisionResult(ReviewData review,RequirementView requirement,VersionData version) {}
    private final ReviewStore store;private final RequirementMutationService requirements;private final VersionAppendService versions;private final AuditWriteService audit;private final Clock clock;
    public ReviewTransactions(ReviewStore store,RequirementMutationService requirements,VersionAppendService versions,AuditWriteService audit,Clock clock) { this.store=store;this.requirements=requirements;this.versions=versions;this.audit=audit;this.clock=clock; }
    @Transactional(rollbackFor=Exception.class)
    public ReviewData submit(Actor actor,long id,String expected) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);long revision=InputPolicy.lockVersion(expected);var before=requirements.lockForReview(id);InputPolicy.expected(revision,before.lockVersion());if(!before.status().equals("DRAFT"))throw new RmsException(ErrorCode.STATE_CONFLICT);if(store.hasPending(id))throw new RmsException(ErrorCode.ACTIVE_REVIEW_EXISTS);before.content().requireComplete();Instant now=now();int round=store.nextRound(id);var parent=requirements.submit(id,revision,now);var result=store.create(id,round,before.content(),actor.userId(),now);var values=safe(result);values.putAll(context(parent));audit.success(actor,new AuditChange(id,"REVIEW",Long.toString(result.reviewId()),"REQUIREMENT_SUBMIT",context(before),values));return result; }
    @Transactional(rollbackFor=Exception.class)
    public DecisionResult decide(Actor actor,long reviewId,String decision,String comment,String expected) {
        actor.requireAny(RoleCode.REVIEWER);InputPolicy.oneOf(decision,"APPROVE","REJECT","REQUEST_CHANGES");long revision=InputPolicy.lockVersion(expected);if(!decision.equals("APPROVE"))InputPolicy.nonBlank(comment,Integer.MAX_VALUE);
        long id=store.requirementId(reviewId);var parent=requirements.lockForReview(id);if(parent.creatorId()==actor.userId())throw new RmsException(ErrorCode.SELF_REVIEW);var before=store.lock(reviewId);InputPolicy.expected(revision,parent.lockVersion());if(!parent.status().equals("UNDER_REVIEW") || !before.reviewStatus().equals("PENDING"))throw new RmsException(ErrorCode.STATE_CONFLICT);
        Instant now=now();var after=store.complete(reviewId,decision,comment,actor.userId(),now);VersionData version=null;
        if(decision.equals("APPROVE")){if(parent.currentVersionId()!=null)throw new RmsException(ErrorCode.STATE_CONFLICT);version=versions.appendInitial(id,reviewId,before.content(),actor.userId(),now);requirements.installInitialVersion(id,version.versionId(),before.content(),now);}else requirements.transitionAfterDecision(id,decision.equals("REQUEST_CHANGES"),now);
        var requirement=requirements.readView(id);var previous=safe(before);previous.putAll(context(parent));var values=safe(after);values.put("status",requirement.status());values.put("currentVersionId",requirement.currentVersionId());values.put("lockVersion",requirement.lockVersion());audit.success(actor,new AuditChange(id,"REVIEW",Long.toString(reviewId),"REQUIREMENT_REVIEW_DECISION",previous,values));return new DecisionResult(after,requirement,version);
    }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static Map<String,Object> context(ReviewRequirement r) { var result=new LinkedHashMap<String,Object>();result.put("status",r.status());result.put("currentVersionId",r.currentVersionId());result.put("lockVersion",r.lockVersion());result.put("firstSubmittedAt",r.firstSubmittedAt()==null?null:r.firstSubmittedAt().toString());return result; }
    private static Map<String,Object> safe(ReviewData r) { var c=r.content();var m=new LinkedHashMap<String,Object>();m.put("reviewId",r.reviewId());m.put("requirementId",r.requirementId());m.put("roundNo",r.roundNo());m.put("snapshotTitle",c.title());m.put("snapshotDescription",c.description());m.put("snapshotLevel",c.level());m.put("snapshotKind",c.kind());m.put("snapshotPriority",c.priority());m.put("snapshotSource",c.source());m.put("snapshotRationale",c.rationale());m.put("snapshotAcceptanceCriteria",c.acceptanceCriteria());m.put("submittedBy",r.submittedBy());m.put("submittedAt",r.submittedAt().toString());m.put("reviewStatus",r.reviewStatus());m.put("reviewerId",r.reviewerId());m.put("decision",r.decision());m.put("comment",r.comment());m.put("decidedAt",r.decidedAt()==null?null:r.decidedAt().toString());return m; }
}

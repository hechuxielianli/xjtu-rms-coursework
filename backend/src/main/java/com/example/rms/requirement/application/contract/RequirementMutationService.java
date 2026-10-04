package com.example.rms.requirement.application.contract;
import com.example.rms.requirement.application.port.RequirementStore;
import com.example.rms.shared.domain.*;
import com.example.rms.requirement.application.RequirementData;
import com.example.rms.user.application.contract.UserAccessService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Owner-controlled context only. No generic status setter or cross-module entity escapes. */
@Service
public class RequirementMutationService {
    private final RequirementStore store;private final UserAccessService users;
    public RequirementMutationService(RequirementStore store,UserAccessService users) { this.store=store;this.users=users; }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public RequirementContext lockContext(long id) { var r=store.lock(id);if(r.isWithdrawn()!=0)throw new RmsException(ErrorCode.STATE_CONFLICT);return new RequirementContext(r.requirementId(),r.status(),false); }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public GraphRequirement lockForGraph(long id) { var r=store.lock(id);active(r);return new GraphRequirement(r.requirementId(),r.lockVersion()); }
    @Transactional(readOnly=true) public RequirementContext readContext(long id) { var r=store.read(id);return new RequirementContext(r.requirementId(),r.status(),r.isWithdrawn()!=0); }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public ReviewRequirement lockForReview(long id) { var r=store.lock(id);active(r);return review(r); }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public ReviewRequirement submit(long id,long expected,Instant now) { var r=store.lock(id);active(r);InputPolicy.expected(expected,r.lockVersion());state(r,"DRAFT");if(r.currentVersionId()!=null)throw new RmsException(ErrorCode.STATE_CONFLICT);review(r).content().requireComplete();return review(store.submitted(id,now)); }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public void transitionAfterDecision(long id,boolean requestChanges,Instant now) { var r=store.lock(id);active(r);state(r,"UNDER_REVIEW");if(r.currentVersionId()!=null)throw new RmsException(ErrorCode.STATE_CONFLICT);store.reviewRejected(id,requestChanges,now); }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public void installInitialVersion(long id,long versionId,ContentSnapshot content,Instant now) { var r=store.lock(id);active(r);state(r,"UNDER_REVIEW");if(r.currentVersionId()!=null)throw new RmsException(ErrorCode.STATE_CONFLICT);if(versionId<=0)throw new RmsException(ErrorCode.INVALID_INPUT);content.requireComplete();store.initialApproved(id,versionId,content,now); }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public FormalRequirement lockFormalContext(long id) { var r=store.lock(id);active(r);if(!java.util.Set.of("APPROVED","IMPLEMENTED","VERIFIED").contains(r.status()) || r.currentVersionId()==null)throw new RmsException(ErrorCode.STATE_CONFLICT);return new FormalRequirement(r.requirementId(),r.status(),r.currentVersionId(),r.lockVersion()); }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class) public void installAppliedVersion(long id,long expectedBase,long versionId,ContentSnapshot content,Instant now) { var r=lockFormalContext(id);if(r.currentVersionId()!=expectedBase)throw new RmsException(ErrorCode.BASE_VERSION_STALE);if(versionId<=0 || versionId==expectedBase)throw new RmsException(ErrorCode.INVALID_INPUT);content.requireComplete();store.appliedVersion(id,versionId,content,now); }
    @Transactional(readOnly=true) public RequirementView readView(long id) { var r=store.read(id);var creator=users.readSummary(r.creatorId());var assignee=r.assigneeId()==null?null:users.readSummary(r.assigneeId());return new RequirementView(r.requirementId(),r.requirementKey(),r.title(),r.description(),r.level(),r.kind(),r.priority(),r.source(),r.rationale(),r.acceptanceCriteria(),r.status(),r.creatorId(),r.assigneeId(),r.currentVersionId(),r.firstSubmittedAt(),r.isWithdrawn(),r.withdrawnBy(),r.withdrawnAt(),r.createdAt(),r.updatedAt(),r.lockVersion(),new RequirementView.Summary(creator.userId(),creator.displayName(),creator.accountStatus()),assignee==null?null:new RequirementView.Summary(assignee.userId(),assignee.displayName(),assignee.accountStatus()),r.tags().stream().map(t->new RequirementView.Tag(t.tagId(),t.name(),t.description(),t.createdBy(),t.createdAt())).toList()); }
    private static ReviewRequirement review(RequirementData r) { return new ReviewRequirement(r.requirementId(),r.creatorId(),r.status(),r.currentVersionId(),r.lockVersion(),r.firstSubmittedAt(),new ContentSnapshot(r.title(),r.description(),r.level(),r.kind(),r.priority(),r.source(),r.rationale(),r.acceptanceCriteria())); }
    private static void active(RequirementData r) { if(r.isWithdrawn()!=0)throw new RmsException(ErrorCode.STATE_CONFLICT); }
    private static void state(RequirementData r,String expected) { if(!r.status().equals(expected))throw new RmsException(ErrorCode.STATE_CONFLICT); }
}

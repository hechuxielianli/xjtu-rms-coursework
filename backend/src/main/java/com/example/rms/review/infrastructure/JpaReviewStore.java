package com.example.rms.review.infrastructure;
import com.example.rms.review.application.ReviewData;
import com.example.rms.review.application.port.ReviewStore;
import com.example.rms.review.infrastructure.persistence.RequirementReviewEntity;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.stereotype.Repository;
@Repository
public class JpaReviewStore implements ReviewStore,com.example.rms.shared.application.FailureTargetContext {
    private final EntityManager em;
    public JpaReviewStore(EntityManager em) { this.em=em; }
    @Override public boolean supports(String type) { return type.equals("REVIEW"); }
    @Override public Long requirementContext(String type,long id) { var rows=em.createQuery("select r.requirementId from RequirementReviewEntity r where r.reviewId=:id",Long.class).setParameter("id",id).getResultList();return rows.isEmpty()?null:rows.getFirst(); }
    /** Locator query does not acquire a child row lock before the owning parent lock. */
    @Override public long requirementId(long id) { var rows=em.createQuery("select r.requirementId from RequirementReviewEntity r where r.reviewId=:id",Long.class).setParameter("id",id).getResultList();if(rows.isEmpty())throw new RmsException(ErrorCode.NOT_FOUND);return rows.getFirst(); }
    @Override public ReviewData lock(long id) { var r=em.find(RequirementReviewEntity.class,id,LockModeType.PESSIMISTIC_WRITE);if(r==null)throw new RmsException(ErrorCode.NOT_FOUND);em.refresh(r,LockModeType.PESSIMISTIC_WRITE);return data(r); }
    @Override public boolean hasPending(long id) { return em.createQuery("select count(r) from RequirementReviewEntity r where r.requirementId=:id and r.reviewStatus='PENDING'",Long.class).setParameter("id",id).getSingleResult()!=0; }
    @Override public int nextRound(long id) { Integer max=em.createQuery("select max(r.roundNo) from RequirementReviewEntity r where r.requirementId=:id",Integer.class).setParameter("id",id).getSingleResult();if(max!=null && max==Integer.MAX_VALUE)throw new RmsException(ErrorCode.STATE_CONFLICT);return max==null?1:max+1; }
    @Override public ReviewData create(long id,int round,ContentSnapshot c,long actorId,Instant now) { var r=RequirementReviewEntity.pending(id,round,c,actorId,now);em.persist(r);em.flush();return data(r); }
    @Override public ReviewData complete(long id,String decision,String comment,long actorId,Instant now) { var r=em.find(RequirementReviewEntity.class,id);r.complete(decision,comment,actorId,now);em.flush();return data(r); }
    @Override public PageData<ReviewData> list(Long id,Paging p) { String filter=id==null?" where r.reviewStatus='PENDING'":" where r.requirementId=:id";var rows=em.createQuery("select r from RequirementReviewEntity r"+filter+" order by r.requirementId,r.roundNo",RequirementReviewEntity.class);var count=em.createQuery("select count(r) from RequirementReviewEntity r"+filter,Long.class);if(id!=null){rows.setParameter("id",id);count.setParameter("id",id);}return new PageData<>(rows.setFirstResult(p.offset()).setMaxResults(p.size()).getResultList().stream().map(JpaReviewStore::data).toList(),p.page(),p.size(),count.getSingleResult()); }
    private static ReviewData data(RequirementReviewEntity r) { return new ReviewData(r.getReviewId(),r.getRequirementId(),r.getRoundNo(),new ContentSnapshot(r.getSnapshotTitle(),r.getSnapshotDescription(),r.getSnapshotLevel(),r.getSnapshotKind(),r.getSnapshotPriority(),r.getSnapshotSource(),r.getSnapshotRationale(),r.getSnapshotAcceptanceCriteria()),r.getSubmittedBy(),r.getSubmittedAt(),r.getReviewStatus(),r.getReviewerId(),r.getDecision(),r.getComment(),r.getDecidedAt()); }
}

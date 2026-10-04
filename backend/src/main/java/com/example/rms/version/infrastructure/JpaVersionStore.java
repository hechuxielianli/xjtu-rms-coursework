package com.example.rms.version.infrastructure;
import com.example.rms.version.application.contract.VersionData;
import com.example.rms.version.application.port.VersionStore;
import com.example.rms.version.infrastructure.persistence.RequirementVersionEntity;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.stereotype.Repository;
@Repository
public class JpaVersionStore implements VersionStore {
    private final EntityManager em;
    public JpaVersionStore(EntityManager em) { this.em=em; }
    /** Narrow frozen-schema existence projection; no foreign owner entity, content or write. */
    @Override public boolean requirementExists(long id) { return ((Number)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM `Requirement` WHERE requirementId=:id)").setParameter("id",id).getSingleResult()).intValue()!=0; }
    @Override public long count(long id) { return em.createQuery("select count(v) from RequirementVersionEntity v where v.requirementId=:id",Long.class).setParameter("id",id).getSingleResult(); }
    @Override public VersionData appendInitial(long id,long reviewId,ContentSnapshot c,long actorId,Instant now,String reason) { var v=RequirementVersionEntity.initial(id,reviewId,c,actorId,now,reason);em.persist(v);em.flush();return data(v); }
    @Override public VersionData appendApplied(long id,long changeId,int nextVersion,ContentSnapshot c,long actorId,Instant now,String reason) { var v=RequirementVersionEntity.applied(id,changeId,nextVersion,c,actorId,now,reason);em.persist(v);em.flush();return data(v); }
    @Override public PageData<VersionData> list(long id,Paging p) { var rows=em.createQuery("select v from RequirementVersionEntity v where v.requirementId=:id order by v.versionNo",RequirementVersionEntity.class).setParameter("id",id).setFirstResult(p.offset()).setMaxResults(p.size()).getResultList().stream().map(JpaVersionStore::data).toList();return new PageData<>(rows,p.page(),p.size(),count(id)); }
    @Override public VersionData read(long id,long versionId) { var rows=em.createQuery("select v from RequirementVersionEntity v where v.requirementId=:id and v.versionId=:version",RequirementVersionEntity.class).setParameter("id",id).setParameter("version",versionId).getResultList();if(rows.isEmpty())throw new RmsException(ErrorCode.NOT_FOUND);return data(rows.getFirst()); }
    private static VersionData data(RequirementVersionEntity v) { return new VersionData(v.getVersionId(),v.getRequirementId(),v.getVersionNo(),new ContentSnapshot(v.getTitle(),v.getDescription(),v.getLevel(),v.getKind(),v.getPriority(),v.getSource(),v.getRationale(),v.getAcceptanceCriteria()),v.getInitialReviewId(),v.getAppliedChangeRequestId(),v.getCreatedBy(),v.getCreatedAt(),v.getChangeReason()); }
}

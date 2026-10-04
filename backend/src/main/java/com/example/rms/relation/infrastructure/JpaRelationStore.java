package com.example.rms.relation.infrastructure;
import com.example.rms.relation.application.*;
import com.example.rms.relation.application.port.RelationStore;
import com.example.rms.relation.infrastructure.persistence.RequirementRelationEntity;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
@Repository
public class JpaRelationStore implements RelationStore,com.example.rms.shared.application.FailureTargetContext {
    private final EntityManager em;public JpaRelationStore(EntityManager em){this.em=em;}
    @Override public boolean supports(String type){return type.equals("RELATION");}
    @Override public Long requirementContext(String type,long id){var rows=em.createQuery("select r.sourceRequirementId from RequirementRelationEntity r where r.relationId=:id",Long.class).setParameter("id",id).getResultList();return rows.isEmpty()?null:rows.getFirst();}
    /** Immutable mappings reject Hibernate pessimistic lock mode; native row lock retains the frozen mapping. */
    private RequirementRelationEntity find(long id,boolean lock) { if(lock){var rows=em.createNativeQuery("SELECT relationId FROM `RequirementRelation` WHERE relationId=:id FOR UPDATE").setParameter("id",id).getResultList();if(rows.isEmpty())throw new RmsException(ErrorCode.NOT_FOUND);}var r=em.find(RequirementRelationEntity.class,id);if(r==null)throw new RmsException(ErrorCode.NOT_FOUND);if(lock)em.refresh(r);return r; }
    @Override public RelationData read(long id){return data(find(id,false));}
    @Override public RelationData lock(long id){return data(find(id,true));}
    @Override public boolean exists(long source,long target,String type){return em.createQuery("select count(r) from RequirementRelationEntity r where r.sourceRequirementId=:s and r.targetRequirementId=:t and r.relationType=:type",Long.class).setParameter("s",source).setParameter("t",target).setParameter("type",type).getSingleResult()!=0;}
    @Override public boolean incident(long id){return em.createQuery("select count(r) from RequirementRelationEntity r where r.sourceRequirementId=:id or r.targetRequirementId=:id",Long.class).setParameter("id",id).getSingleResult()!=0;}
    @Override public List<Long> outgoingDependencies(long id){return em.createQuery("select r.targetRequirementId from RequirementRelationEntity r where r.sourceRequirementId=:id and r.relationType='DEPENDS_ON' order by r.relationId",Long.class).setParameter("id",id).getResultList();}
    @Override public List<RelationData> incident(long id,String type){String condition=type==null?"":" and r.relationType=:type";var q=em.createQuery("select r from RequirementRelationEntity r where (r.sourceRequirementId=:id or r.targetRequirementId=:id)"+condition+" order by r.relationId",RequirementRelationEntity.class).setParameter("id",id);if(type!=null)q.setParameter("type",type);return q.getResultList().stream().map(JpaRelationStore::data).toList();}
    @Override public PageData<RelationData> list(long id,Paging p){var q=em.createQuery("select r from RequirementRelationEntity r where r.sourceRequirementId=:id or r.targetRequirementId=:id order by r.relationId",RequirementRelationEntity.class).setParameter("id",id);var n=em.createQuery("select count(r) from RequirementRelationEntity r where r.sourceRequirementId=:id or r.targetRequirementId=:id",Long.class).setParameter("id",id).getSingleResult();return new PageData<>(q.setFirstResult(p.offset()).setMaxResults(p.size()).getResultList().stream().map(JpaRelationStore::data).toList(),p.page(),p.size(),n);}
    @Override public RelationData create(long source,long target,String type,String description,long actorId,Instant now){var r=RequirementRelationEntity.create(source,target,type,description,actorId,now);em.persist(r);em.flush();return data(r);}
    @Override public void remove(long id){em.remove(find(id,false));em.flush();}
    private static RelationData data(RequirementRelationEntity r){return new RelationData(r.getRelationId(),r.getSourceRequirementId(),r.getTargetRequirementId(),r.getRelationType(),r.getDescription(),r.getCreatedBy(),r.getCreatedAt());}
}

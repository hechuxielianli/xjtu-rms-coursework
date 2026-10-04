package com.example.rms.audit.infrastructure;
import com.example.rms.audit.application.*;
import com.example.rms.audit.application.contract.AuditChange;
import com.example.rms.audit.application.port.AuditQueryPort;
import com.example.rms.audit.infrastructure.persistence.AuditEventEntity;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.Paging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class JpaAuditQuery implements AuditQueryPort {
    private final EntityManager em;private final ObjectMapper json;
    public JpaAuditQuery(EntityManager em,ObjectMapper json) { this.em=em;this.json=json; }
    /** Frozen physical-schema PK EXISTS only; no foreign Java entity, content or mutation. */
    @Override public boolean requirementExists(long id) { return ((Number)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM `Requirement` WHERE requirementId=:id)").setParameter("id",id).getSingleResult()).intValue()!=0; }
    @Override public PageData<AuditData> query(Long requirementId,Paging p,AuditFilter filter) {
        StringBuilder where=new StringBuilder(" where 1=1");Map<String,Object> parameters=new LinkedHashMap<>();
        if(requirementId!=null){where.append(" and a.requirementId=:requirement");parameters.put("requirement",requirementId);}
        if(filter.from()!=null){where.append(" and a.occurredAt>=:fromTime");parameters.put("fromTime",filter.from());}
        if(filter.to()!=null){where.append(" and a.occurredAt<=:toTime");parameters.put("toTime",filter.to());}
        if(filter.action()!=null){where.append(" and a.action=:action");parameters.put("action",filter.action());}
        var count=em.createQuery("select count(a) from AuditEventEntity a"+where,Long.class);parameters.forEach(count::setParameter);
        var rows=em.createQuery("select a from AuditEventEntity a"+where+" order by a.occurredAt,a.auditId",AuditEventEntity.class);parameters.forEach(rows::setParameter);
        return new PageData<>(rows.setFirstResult(p.offset()).setMaxResults(p.size()).getResultList().stream().map(this::data).toList(),p.page(),p.size(),count.getSingleResult());
    }
    private AuditData data(AuditEventEntity e) {
        // Apply the same explicit field/value whitelist again at the read boundary.
        var safe=new AuditChange(e.getRequirementId(),e.getTargetType(),e.getTargetId(),e.getAction(),decode(e.getBeforeData()),decode(e.getAfterData()));
        if(!Set.of("SUCCESS","DENIED","FAILED").contains(e.getOutcome()))throw new IllegalStateException("INVALID_STORED_AUDIT_OUTCOME");
        return new AuditData(e.getAuditId(),e.getActorId(),e.getRequirementId(),e.getTargetType(),e.getTargetId(),e.getAction(),e.getOutcome(),safe.before(),safe.after(),e.getOccurredAt());
    }
    private Map<String,Object> decode(String value) {
        if(value==null)return null;
        try { return json.readValue(value,new TypeReference<Map<String,Object>>(){}); }
        catch(JsonProcessingException e) { throw new IllegalStateException("INVALID_STORED_AUDIT_PROJECTION"); }
    }
}

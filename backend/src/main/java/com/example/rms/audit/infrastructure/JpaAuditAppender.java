package com.example.rms.audit.infrastructure;
import com.example.rms.audit.application.port.AuditAppendPort;
import com.example.rms.audit.domain.*;
import com.example.rms.audit.infrastructure.persistence.AuditEventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
@Component
public class JpaAuditAppender implements AuditAppendPort {
    private final EntityManager em;
    private final ObjectMapper json;
    public JpaAuditAppender(EntityManager em,ObjectMapper json) { this.em=em;this.json=json; }
    @Override public void append(AuditEntry entry) {
        try { em.persist(AuditEventEntity.append(entry.actorId(),entry.requirementId(),entry.targetType(),entry.targetId(),entry.action(),entry.outcome(),entry.before()==null?null:json.writeValueAsString(entry.before()),entry.after()==null?null:json.writeValueAsString(entry.after()),entry.occurredAt())); }
        catch(JsonProcessingException e) { throw new IllegalStateException("AUDIT_PROJECTION_ENCODING_FAILED"); }
    }
}

package com.example.rms.audit.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="AuditEvent")
@org.hibernate.annotations.Immutable
public class AuditEventEntity {
    protected AuditEventEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="auditId", nullable=false, updatable=false)
    private Long auditId;
    @Column(name="actorId", nullable=false, updatable=false)
    private Long actorId;
    @Column(name="requirementId", nullable=true, updatable=false)
    private Long requirementId;
    @Column(name="targetType", nullable=false, length=32, updatable=false)
    private String targetType;
    @Column(name="targetId", nullable=false, length=128, updatable=false)
    private String targetId;
    @Column(name="action", nullable=false, length=64, updatable=false)
    private String action;
    @Column(name="outcome", nullable=false, length=32, updatable=false)
    private String outcome;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="beforeData", nullable=true, columnDefinition="json", updatable=false)
    private String beforeData;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="afterData", nullable=true, columnDefinition="json", updatable=false)
    private String afterData;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="occurredAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant occurredAt;
    public Long getAuditId() { return auditId; }
    public Long getActorId() { return actorId; }
    public Long getRequirementId() { return requirementId; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getAction() { return action; }
    public String getOutcome() { return outcome; }
    public String getBeforeData() { return beforeData; }
    public String getAfterData() { return afterData; }
    public Instant getOccurredAt() { return occurredAt; }
    public static AuditEventEntity append(Long actorId,Long requirementId,String targetType,String targetId,String action,String outcome,String before,String after,Instant now) {
        AuditEventEntity e=new AuditEventEntity();e.actorId=actorId;e.requirementId=requirementId;e.targetType=targetType;e.targetId=targetId;e.action=action;e.outcome=outcome;e.beforeData=before;e.afterData=after;e.occurredAt=now;return e;
    }
}

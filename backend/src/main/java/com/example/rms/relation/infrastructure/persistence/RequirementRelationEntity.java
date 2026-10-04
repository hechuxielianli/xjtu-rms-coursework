package com.example.rms.relation.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="RequirementRelation")
@org.hibernate.annotations.Immutable
public class RequirementRelationEntity {
    protected RequirementRelationEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="relationId", nullable=false, updatable=false)
    private Long relationId;
    @Column(name="sourceRequirementId", nullable=false, updatable=false)
    private Long sourceRequirementId;
    @Column(name="targetRequirementId", nullable=false, updatable=false)
    private Long targetRequirementId;
    @Column(name="relationType", nullable=false, length=32, updatable=false)
    private String relationType;
    @Column(name="description", nullable=true, length=1000, updatable=false)
    private String description;
    @Column(name="createdBy", nullable=false, updatable=false)
    private Long createdBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="createdAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant createdAt;
    public static RequirementRelationEntity create(long source,long target,String type,String description,long actorId,Instant now) { var r=new RequirementRelationEntity();r.sourceRequirementId=source;r.targetRequirementId=target;r.relationType=type;r.description=description;r.createdBy=actorId;r.createdAt=now;return r; }
    public Long getRelationId() { return relationId; }
    public Long getSourceRequirementId() { return sourceRequirementId; }
    public Long getTargetRequirementId() { return targetRequirementId; }
    public String getRelationType() { return relationType; }
    public String getDescription() { return description; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}

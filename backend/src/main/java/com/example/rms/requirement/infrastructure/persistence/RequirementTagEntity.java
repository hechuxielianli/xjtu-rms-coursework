package com.example.rms.requirement.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="RequirementTag")
@IdClass(RequirementTagId.class)
@org.hibernate.annotations.Immutable
public class RequirementTagEntity {
    protected RequirementTagEntity() {}
    @Id
    @Column(name="requirementId", nullable=false, updatable=false)
    private Long requirementId;
    @Id
    @Column(name="tagId", nullable=false, updatable=false)
    private Long tagId;
    @Column(name="addedBy", nullable=false, updatable=false)
    private Long addedBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="addedAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant addedAt;
    public Long getRequirementId() { return requirementId; }
    public Long getTagId() { return tagId; }
    public Long getAddedBy() { return addedBy; }
    public Instant getAddedAt() { return addedAt; }
    public static RequirementTagEntity add(long requirementId,long tagId,long actorId,Instant now) { RequirementTagEntity t=new RequirementTagEntity();t.requirementId=requirementId;t.tagId=tagId;t.addedBy=actorId;t.addedAt=now;return t; }
}

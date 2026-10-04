package com.example.rms.version.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="RequirementVersion")
@org.hibernate.annotations.Immutable
public class RequirementVersionEntity {
    protected RequirementVersionEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="versionId", nullable=false, updatable=false)
    private Long versionId;
    @Column(name="requirementId", nullable=false, updatable=false)
    private Long requirementId;
    @Column(name="versionNo", nullable=false, updatable=false)
    private Integer versionNo;
    @Column(name="title", nullable=false, length=200, updatable=false)
    private String title;
    @Column(name="description", nullable=false, columnDefinition="text", updatable=false)
    private String description;
    @Column(name="level", nullable=false, length=32, updatable=false)
    private String level;
    @Column(name="kind", nullable=false, length=32, updatable=false)
    private String kind;
    @Column(name="priority", nullable=false, length=32, updatable=false)
    private String priority;
    @Column(name="source", nullable=false, columnDefinition="text", updatable=false)
    private String source;
    @Column(name="rationale", nullable=false, columnDefinition="text", updatable=false)
    private String rationale;
    @Column(name="acceptanceCriteria", nullable=false, columnDefinition="text", updatable=false)
    private String acceptanceCriteria;
    @Column(name="initialReviewId", nullable=true, updatable=false)
    private Long initialReviewId;
    @Column(name="appliedChangeRequestId", nullable=true, updatable=false)
    private Long appliedChangeRequestId;
    @Column(name="createdBy", nullable=false, updatable=false)
    private Long createdBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="createdAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant createdAt;
    @Column(name="changeReason", nullable=false, columnDefinition="text", updatable=false)
    private String changeReason;
    public Long getVersionId() { return versionId; }
    public Long getRequirementId() { return requirementId; }
    public Integer getVersionNo() { return versionNo; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLevel() { return level; }
    public String getKind() { return kind; }
    public String getPriority() { return priority; }
    public String getSource() { return source; }
    public String getRationale() { return rationale; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public Long getInitialReviewId() { return initialReviewId; }
    public Long getAppliedChangeRequestId() { return appliedChangeRequestId; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getChangeReason() { return changeReason; }
    public static RequirementVersionEntity initial(long requirementId,long reviewId,com.example.rms.shared.domain.ContentSnapshot c,long actorId,Instant now,String reason) { var v=new RequirementVersionEntity();v.requirementId=requirementId;v.versionNo=1;v.title=c.title();v.description=c.description();v.level=c.level();v.kind=c.kind();v.priority=c.priority();v.source=c.source();v.rationale=c.rationale();v.acceptanceCriteria=c.acceptanceCriteria();v.initialReviewId=reviewId;v.appliedChangeRequestId=null;v.createdBy=actorId;v.createdAt=now;v.changeReason=reason;return v; }
    public static RequirementVersionEntity applied(long requirementId,long changeId,int nextVersion,com.example.rms.shared.domain.ContentSnapshot c,long actorId,Instant now,String reason) { var v=initial(requirementId,0,c,actorId,now,reason);v.versionNo=nextVersion;v.initialReviewId=null;v.appliedChangeRequestId=changeId;return v; }
}

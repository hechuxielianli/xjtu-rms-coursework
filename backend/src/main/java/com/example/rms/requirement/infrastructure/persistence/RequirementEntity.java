package com.example.rms.requirement.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="Requirement")
public class RequirementEntity {
    protected RequirementEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="requirementId", nullable=false, updatable=false)
    private Long requirementId;
    @Column(name="requirementKey", nullable=false, length=32, updatable=false)
    private String requirementKey;
    @Column(name="title", nullable=false, length=200)
    private String title;
    @Column(name="description", nullable=false, columnDefinition="text")
    private String description;
    @Column(name="level", nullable=false, length=32)
    private String level;
    @Column(name="kind", nullable=false, length=32)
    private String kind;
    @Column(name="priority", nullable=false, length=32)
    private String priority;
    @Column(name="source", nullable=true, columnDefinition="text")
    private String source;
    @Column(name="rationale", nullable=true, columnDefinition="text")
    private String rationale;
    @Column(name="acceptanceCriteria", nullable=true, columnDefinition="text")
    private String acceptanceCriteria;
    @Column(name="status", nullable=false, length=32)
    private String status;
    @Column(name="creatorId", nullable=false, updatable=false)
    private Long creatorId;
    @Column(name="assigneeId", nullable=true)
    private Long assigneeId;
    @Column(name="currentVersionId", nullable=true)
    private Long currentVersionId;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="firstSubmittedAt", nullable=true, columnDefinition="datetime(6)")
    private Instant firstSubmittedAt;
    @Column(name="isWithdrawn", nullable=false)
    private Byte isWithdrawn;
    @Column(name="withdrawnBy", nullable=true)
    private Long withdrawnBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="withdrawnAt", nullable=true, columnDefinition="datetime(6)")
    private Instant withdrawnAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="createdAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="updatedAt", nullable=false, columnDefinition="datetime(6)")
    private Instant updatedAt;
    @Version
    @Column(name="lockVersion", nullable=false)
    private Long lockVersion;
    public Long getRequirementId() { return requirementId; }
    public String getRequirementKey() { return requirementKey; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLevel() { return level; }
    public String getKind() { return kind; }
    public String getPriority() { return priority; }
    public String getSource() { return source; }
    public String getRationale() { return rationale; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public String getStatus() { return status; }
    public Long getCreatorId() { return creatorId; }
    public Long getAssigneeId() { return assigneeId; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public Instant getFirstSubmittedAt() { return firstSubmittedAt; }
    public Byte getIsWithdrawn() { return isWithdrawn; }
    public Long getWithdrawnBy() { return withdrawnBy; }
    public Instant getWithdrawnAt() { return withdrawnAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getLockVersion() { return lockVersion; }
    public static RequirementEntity draft(String key,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,long actorId,Instant now) {
        RequirementEntity r=new RequirementEntity();r.requirementKey=key;r.title=title;r.description=description;r.level=level;r.kind=kind;r.priority=priority;r.source=source;r.rationale=rationale;r.acceptanceCriteria=acceptanceCriteria;r.status="DRAFT";r.creatorId=actorId;r.isWithdrawn=0;r.createdAt=now;r.updatedAt=now;r.lockVersion=0L;return r;
    }
    public void editDraft(String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,Instant now) { this.title=title;this.description=description;this.level=level;this.kind=kind;this.priority=priority;this.source=source;this.rationale=rationale;this.acceptanceCriteria=acceptanceCriteria;touch(now); }
    public void assign(Long assigneeId,Instant now) { this.assigneeId=assigneeId;touch(now); }
    public void submitted(Instant now) { status="UNDER_REVIEW";if(firstSubmittedAt==null)firstSubmittedAt=now;touch(now); }
    public void reviewRejected(boolean requestChanges,Instant now) { status=requestChanges?"DRAFT":"REJECTED";touch(now); }
    public void initialApproved(long versionId,com.example.rms.shared.domain.ContentSnapshot c,Instant now) { title=c.title();description=c.description();level=c.level();kind=c.kind();priority=c.priority();source=c.source();rationale=c.rationale();acceptanceCriteria=c.acceptanceCriteria();currentVersionId=versionId;status="APPROVED";touch(now); }
    public void appliedVersion(long versionId,com.example.rms.shared.domain.ContentSnapshot c,Instant now) { initialApproved(versionId,c,now); }
    public void withdraw(long actorId,Instant now) { isWithdrawn=1;withdrawnBy=actorId;withdrawnAt=now;touch(now); }
    public void reopen(Instant now) { status="DRAFT";touch(now); }
    public void confirm(boolean verify,Instant now) { status=verify?"VERIFIED":"IMPLEMENTED";touch(now); }
    public void touch(Instant now) { this.updatedAt=now.isAfter(updatedAt)?now:updatedAt.plus(1,java.time.temporal.ChronoUnit.MICROS); }
}

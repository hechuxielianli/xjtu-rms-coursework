package com.example.rms.change.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="ChangeRequest")
public class ChangeRequestEntity {
    protected ChangeRequestEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="changeRequestId", nullable=false, updatable=false)
    private Long changeRequestId;
    @Column(name="requirementId", nullable=false, updatable=false)
    private Long requirementId;
    @Column(name="baseVersionId", nullable=false, updatable=false)
    private Long baseVersionId;
    @Column(name="requestTitle", nullable=false, length=200)
    private String requestTitle;
    @Column(name="reason", nullable=false, columnDefinition="text")
    private String reason;
    @Column(name="proposedTitle", nullable=false, length=200)
    private String proposedTitle;
    @Column(name="proposedDescription", nullable=false, columnDefinition="text")
    private String proposedDescription;
    @Column(name="proposedLevel", nullable=false, length=32)
    private String proposedLevel;
    @Column(name="proposedKind", nullable=false, length=32)
    private String proposedKind;
    @Column(name="proposedPriority", nullable=false, length=32)
    private String proposedPriority;
    @Column(name="proposedSource", nullable=true, columnDefinition="text")
    private String proposedSource;
    @Column(name="proposedRationale", nullable=true, columnDefinition="text")
    private String proposedRationale;
    @Column(name="proposedAcceptanceCriteria", nullable=true, columnDefinition="text")
    private String proposedAcceptanceCriteria;
    @Column(name="status", nullable=false, length=32)
    private String status;
    @Column(name="createdBy", nullable=false, updatable=false)
    private Long createdBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="createdAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="updatedAt", nullable=false, columnDefinition="datetime(6)")
    private Instant updatedAt;
    @Column(name="appliedBy", nullable=true)
    private Long appliedBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="appliedAt", nullable=true, columnDefinition="datetime(6)")
    private Instant appliedAt;
    @Version
    @Column(name="lockVersion", nullable=false)
    private Long lockVersion;
    @Column(name="activeRequirementId", nullable=true, insertable=false, updatable=false)
    private Long activeRequirementId;
    public Long getChangeRequestId() { return changeRequestId; }
    public Long getRequirementId() { return requirementId; }
    public Long getBaseVersionId() { return baseVersionId; }
    public String getRequestTitle() { return requestTitle; }
    public String getReason() { return reason; }
    public String getProposedTitle() { return proposedTitle; }
    public String getProposedDescription() { return proposedDescription; }
    public String getProposedLevel() { return proposedLevel; }
    public String getProposedKind() { return proposedKind; }
    public String getProposedPriority() { return proposedPriority; }
    public String getProposedSource() { return proposedSource; }
    public String getProposedRationale() { return proposedRationale; }
    public String getProposedAcceptanceCriteria() { return proposedAcceptanceCriteria; }
    public String getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getAppliedBy() { return appliedBy; }
    public Instant getAppliedAt() { return appliedAt; }
    public Long getLockVersion() { return lockVersion; }
    public Long getActiveRequirementId() { return activeRequirementId; }
    public static ChangeRequestEntity draft(long requirementId,long baseVersionId,String title,String reason,com.example.rms.shared.domain.ContentSnapshot c,long actorId,Instant now) { var r=new ChangeRequestEntity();r.requirementId=requirementId;r.baseVersionId=baseVersionId;r.requestTitle=title;r.reason=reason;r.proposedTitle=c.title();r.proposedDescription=c.description();r.proposedLevel=c.level();r.proposedKind=c.kind();r.proposedPriority=c.priority();r.proposedSource=c.source();r.proposedRationale=c.rationale();r.proposedAcceptanceCriteria=c.acceptanceCriteria();r.status="DRAFT";r.createdBy=actorId;r.createdAt=now;r.updatedAt=now;r.lockVersion=0L;return r; }
    public void edit(String title,String reason,com.example.rms.shared.domain.ContentSnapshot c,Instant now) { require("DRAFT");requestTitle=title;this.reason=reason;proposedTitle=c.title();proposedDescription=c.description();proposedLevel=c.level();proposedKind=c.kind();proposedPriority=c.priority();proposedSource=c.source();proposedRationale=c.rationale();proposedAcceptanceCriteria=c.acceptanceCriteria();touch(now); }
    public void submit(Instant now) { require("DRAFT");status="UNDER_REVIEW";touch(now); }
    public void cancel(Instant now) { require("DRAFT");status="CANCELLED";touch(now); }
    public void afterDecision(String decision,Instant now) { require("UNDER_REVIEW");status=switch(decision){case "APPROVE"->"APPROVED";case "REJECT"->"REJECTED";case "REQUEST_CHANGES"->"DRAFT";default->throw new com.example.rms.shared.domain.RmsException(com.example.rms.shared.domain.ErrorCode.INVALID_INPUT);};touch(now); }
    public void applied(long actorId,Instant now) { require("APPROVED");status="APPLIED";appliedBy=actorId;appliedAt=now;touch(now); }
    private void require(String expected) { if(!expected.equals(status))throw new com.example.rms.shared.domain.RmsException(com.example.rms.shared.domain.ErrorCode.STATE_CONFLICT); }
    private void touch(Instant now) { updatedAt=now.isAfter(updatedAt)?now:updatedAt.plus(1,java.time.temporal.ChronoUnit.MICROS); }
}

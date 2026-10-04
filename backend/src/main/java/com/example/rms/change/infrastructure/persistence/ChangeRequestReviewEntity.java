package com.example.rms.change.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="ChangeRequestReview")
public class ChangeRequestReviewEntity {
    protected ChangeRequestReviewEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="changeReviewId", nullable=false, updatable=false)
    private Long changeReviewId;
    @Column(name="requirementId", nullable=false, updatable=false)
    private Long requirementId;
    @Column(name="changeRequestId", nullable=false, updatable=false)
    private Long changeRequestId;
    @Column(name="roundNo", nullable=false, updatable=false)
    private Integer roundNo;
    @Column(name="snapshotBaseVersionId", nullable=false, updatable=false)
    private Long snapshotBaseVersionId;
    @Column(name="snapshotRequestTitle", nullable=false, length=200, updatable=false)
    private String snapshotRequestTitle;
    @Column(name="snapshotReason", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotReason;
    @Column(name="snapshotProposedTitle", nullable=false, length=200, updatable=false)
    private String snapshotProposedTitle;
    @Column(name="snapshotProposedDescription", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotProposedDescription;
    @Column(name="snapshotProposedLevel", nullable=false, length=32, updatable=false)
    private String snapshotProposedLevel;
    @Column(name="snapshotProposedKind", nullable=false, length=32, updatable=false)
    private String snapshotProposedKind;
    @Column(name="snapshotProposedPriority", nullable=false, length=32, updatable=false)
    private String snapshotProposedPriority;
    @Column(name="snapshotProposedSource", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotProposedSource;
    @Column(name="snapshotProposedRationale", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotProposedRationale;
    @Column(name="snapshotProposedAcceptanceCriteria", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotProposedAcceptanceCriteria;
    @Column(name="submittedBy", nullable=false, updatable=false)
    private Long submittedBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="submittedAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant submittedAt;
    @Column(name="reviewStatus", nullable=false, length=32)
    private String reviewStatus;
    @Column(name="reviewerId", nullable=true)
    private Long reviewerId;
    @Column(name="decision", nullable=true, length=32)
    private String decision;
    @Column(name="comment", nullable=true, columnDefinition="text")
    private String comment;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="decidedAt", nullable=true, columnDefinition="datetime(6)")
    private Instant decidedAt;
    @Column(name="activeChangeRequestId", nullable=true, insertable=false, updatable=false)
    private Long activeChangeRequestId;
    public Long getChangeReviewId() { return changeReviewId; }
    public Long getRequirementId() { return requirementId; }
    public Long getChangeRequestId() { return changeRequestId; }
    public Integer getRoundNo() { return roundNo; }
    public Long getSnapshotBaseVersionId() { return snapshotBaseVersionId; }
    public String getSnapshotRequestTitle() { return snapshotRequestTitle; }
    public String getSnapshotReason() { return snapshotReason; }
    public String getSnapshotProposedTitle() { return snapshotProposedTitle; }
    public String getSnapshotProposedDescription() { return snapshotProposedDescription; }
    public String getSnapshotProposedLevel() { return snapshotProposedLevel; }
    public String getSnapshotProposedKind() { return snapshotProposedKind; }
    public String getSnapshotProposedPriority() { return snapshotProposedPriority; }
    public String getSnapshotProposedSource() { return snapshotProposedSource; }
    public String getSnapshotProposedRationale() { return snapshotProposedRationale; }
    public String getSnapshotProposedAcceptanceCriteria() { return snapshotProposedAcceptanceCriteria; }
    public Long getSubmittedBy() { return submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getReviewStatus() { return reviewStatus; }
    public Long getReviewerId() { return reviewerId; }
    public String getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getDecidedAt() { return decidedAt; }
    public Long getActiveChangeRequestId() { return activeChangeRequestId; }
    public static ChangeRequestReviewEntity pending(long requirementId,long changeId,int round,long base,String title,String reason,com.example.rms.shared.domain.ContentSnapshot c,long actorId,Instant now) { var r=new ChangeRequestReviewEntity();r.requirementId=requirementId;r.changeRequestId=changeId;r.roundNo=round;r.snapshotBaseVersionId=base;r.snapshotRequestTitle=title;r.snapshotReason=reason;r.snapshotProposedTitle=c.title();r.snapshotProposedDescription=c.description();r.snapshotProposedLevel=c.level();r.snapshotProposedKind=c.kind();r.snapshotProposedPriority=c.priority();r.snapshotProposedSource=c.source();r.snapshotProposedRationale=c.rationale();r.snapshotProposedAcceptanceCriteria=c.acceptanceCriteria();r.submittedBy=actorId;r.submittedAt=now;r.reviewStatus="PENDING";return r; }
    public void complete(String decision,String comment,long actorId,Instant now) { if(!"PENDING".equals(reviewStatus))throw new com.example.rms.shared.domain.RmsException(com.example.rms.shared.domain.ErrorCode.STATE_CONFLICT);reviewStatus="COMPLETED";this.decision=decision;this.comment=comment;reviewerId=actorId;decidedAt=now; }

}

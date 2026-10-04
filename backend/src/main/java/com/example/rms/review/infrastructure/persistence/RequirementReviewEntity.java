package com.example.rms.review.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="RequirementReview")
public class RequirementReviewEntity {
    protected RequirementReviewEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="reviewId", nullable=false, updatable=false)
    private Long reviewId;
    @Column(name="requirementId", nullable=false, updatable=false)
    private Long requirementId;
    @Column(name="roundNo", nullable=false, updatable=false)
    private Integer roundNo;
    @Column(name="snapshotTitle", nullable=false, length=200, updatable=false)
    private String snapshotTitle;
    @Column(name="snapshotDescription", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotDescription;
    @Column(name="snapshotLevel", nullable=false, length=32, updatable=false)
    private String snapshotLevel;
    @Column(name="snapshotKind", nullable=false, length=32, updatable=false)
    private String snapshotKind;
    @Column(name="snapshotPriority", nullable=false, length=32, updatable=false)
    private String snapshotPriority;
    @Column(name="snapshotSource", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotSource;
    @Column(name="snapshotRationale", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotRationale;
    @Column(name="snapshotAcceptanceCriteria", nullable=false, columnDefinition="text", updatable=false)
    private String snapshotAcceptanceCriteria;
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
    @Column(name="activeRequirementId", nullable=true, insertable=false, updatable=false)
    private Long activeRequirementId;
    public Long getReviewId() { return reviewId; }
    public Long getRequirementId() { return requirementId; }
    public Integer getRoundNo() { return roundNo; }
    public String getSnapshotTitle() { return snapshotTitle; }
    public String getSnapshotDescription() { return snapshotDescription; }
    public String getSnapshotLevel() { return snapshotLevel; }
    public String getSnapshotKind() { return snapshotKind; }
    public String getSnapshotPriority() { return snapshotPriority; }
    public String getSnapshotSource() { return snapshotSource; }
    public String getSnapshotRationale() { return snapshotRationale; }
    public String getSnapshotAcceptanceCriteria() { return snapshotAcceptanceCriteria; }
    public Long getSubmittedBy() { return submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getReviewStatus() { return reviewStatus; }
    public Long getReviewerId() { return reviewerId; }
    public String getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getDecidedAt() { return decidedAt; }
    public Long getActiveRequirementId() { return activeRequirementId; }
    public static RequirementReviewEntity pending(long id,int round,com.example.rms.shared.domain.ContentSnapshot c,long actorId,Instant now) { var r=new RequirementReviewEntity();r.requirementId=id;r.roundNo=round;r.snapshotTitle=c.title();r.snapshotDescription=c.description();r.snapshotLevel=c.level();r.snapshotKind=c.kind();r.snapshotPriority=c.priority();r.snapshotSource=c.source();r.snapshotRationale=c.rationale();r.snapshotAcceptanceCriteria=c.acceptanceCriteria();r.submittedBy=actorId;r.submittedAt=now;r.reviewStatus="PENDING";return r; }
    public void complete(String decision,String comment,long actorId,Instant now) { if(!"PENDING".equals(reviewStatus))throw new com.example.rms.shared.domain.RmsException(com.example.rms.shared.domain.ErrorCode.STATE_CONFLICT);reviewStatus="COMPLETED";this.decision=decision;this.comment=comment;reviewerId=actorId;decidedAt=now; }
}

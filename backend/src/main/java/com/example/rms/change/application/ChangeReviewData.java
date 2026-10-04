package com.example.rms.change.application;
import com.example.rms.shared.domain.ContentSnapshot;
import java.time.Instant;
public record ChangeReviewData(long changeReviewId,long requirementId,long changeRequestId,int roundNo,long snapshotBaseVersionId,String snapshotRequestTitle,String snapshotReason,ContentSnapshot proposed,long submittedBy,Instant submittedAt,String reviewStatus,Long reviewerId,String decision,String comment,Instant decidedAt) {}

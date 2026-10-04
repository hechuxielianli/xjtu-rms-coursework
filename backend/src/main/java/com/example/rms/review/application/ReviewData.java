package com.example.rms.review.application;
import com.example.rms.shared.domain.ContentSnapshot;
import java.time.Instant;
public record ReviewData(long reviewId,long requirementId,int roundNo,ContentSnapshot content,long submittedBy,Instant submittedAt,String reviewStatus,Long reviewerId,String decision,String comment,Instant decidedAt) {}

package com.example.rms.version.application.contract;
import com.example.rms.shared.domain.ContentSnapshot;
import java.time.Instant;
public record VersionData(long versionId,long requirementId,int versionNo,ContentSnapshot content,Long initialReviewId,Long appliedChangeRequestId,long createdBy,Instant createdAt,String changeReason) {}

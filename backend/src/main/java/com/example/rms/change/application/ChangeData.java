package com.example.rms.change.application;
import com.example.rms.shared.domain.ContentSnapshot;
import java.time.Instant;
public record ChangeData(long changeRequestId,long requirementId,long baseVersionId,String requestTitle,String reason,ContentSnapshot proposed,String status,long createdBy,Instant createdAt,Instant updatedAt,Long appliedBy,Instant appliedAt,long lockVersion) {}

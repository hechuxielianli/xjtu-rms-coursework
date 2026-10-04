package com.example.rms.requirement.application;
import java.time.Instant;
import java.util.List;
public record RequirementData(long requirementId,String requirementKey,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,String status,long creatorId,Long assigneeId,Long currentVersionId,Instant firstSubmittedAt,byte isWithdrawn,Long withdrawnBy,Instant withdrawnAt,Instant createdAt,Instant updatedAt,long lockVersion,List<TagData> tags) {}

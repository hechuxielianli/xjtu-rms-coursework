package com.example.rms.requirement.application.contract;
import java.time.Instant;
import java.util.List;
/** Safe public owner projection; IDs are serialized by the common BIGINT policy. */
public record RequirementView(long requirementId,String requirementKey,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,String status,long creatorId,Long assigneeId,Long currentVersionId,Instant firstSubmittedAt,byte isWithdrawn,Long withdrawnBy,Instant withdrawnAt,Instant createdAt,Instant updatedAt,long lockVersion,Summary creator,Summary assignee,List<Tag> tags) {
    public record Summary(long userId,String displayName,String accountStatus) {}
    public record Tag(long tagId,String name,String description,long createdBy,Instant createdAt) {}
}

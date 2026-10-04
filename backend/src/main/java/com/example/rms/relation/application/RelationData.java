package com.example.rms.relation.application;
import java.time.Instant;
public record RelationData(long relationId,long sourceRequirementId,long targetRequirementId,String relationType,String description,long createdBy,Instant createdAt) {}

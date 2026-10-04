package com.example.rms.requirement.application.contract;
import com.example.rms.shared.domain.ContentSnapshot;
import java.time.Instant;
public record ReviewRequirement(long requirementId,long creatorId,String status,Long currentVersionId,long lockVersion,Instant firstSubmittedAt,ContentSnapshot content) {}

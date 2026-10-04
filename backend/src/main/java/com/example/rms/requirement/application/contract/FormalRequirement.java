package com.example.rms.requirement.application.contract;
public record FormalRequirement(long requirementId,String status,long currentVersionId,long lockVersion) {}

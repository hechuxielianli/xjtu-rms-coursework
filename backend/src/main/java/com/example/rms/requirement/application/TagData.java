package com.example.rms.requirement.application;
import java.time.Instant;
public record TagData(long tagId,String name,String description,long createdBy,Instant createdAt) {}

package com.example.rms.audit.domain;
import java.time.Instant;
import java.util.Map;
public record AuditEntry(long actorId,Long requirementId,String targetType,String targetId,String action,String outcome,Map<String,Object> before,Map<String,Object> after,Instant occurredAt) {}


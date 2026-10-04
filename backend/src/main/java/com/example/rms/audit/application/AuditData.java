package com.example.rms.audit.application;
import java.time.Instant;
import java.util.Map;
/** Read-only owner projection; no credentials, entity or request crosses the boundary. */
public record AuditData(long auditId,long actorId,Long requirementId,String targetType,String targetId,String action,String outcome,Map<String,Object> beforeData,Map<String,Object> afterData,Instant occurredAt) {}

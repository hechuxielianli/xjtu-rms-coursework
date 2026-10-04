package com.example.rms.audit.application.contract;
import java.util.*;
/** Owner supplies only explicit safe before/after projections, never an entity or HTTP request. */
public record AuditChange(Long requirementId,String targetType,String targetId,String action,Map<String,Object> before,Map<String,Object> after) {
    /** A locator candidate only; the independent failure transaction must verify the parent FK. */
    public static Long requirementCandidate(String raw) {
        try { return com.example.rms.shared.domain.InputPolicy.decimalId(raw); }
        catch(com.example.rms.shared.domain.RmsException invalid) { return null; }
    }
    private static final Map<String,Set<String>> FIELDS=Map.ofEntries(
        Map.entry("USER",Set.of("userId","username","email","displayName","accountStatus","roles","lockVersion")),
        Map.entry("ROLE_ASSIGNMENT",Set.of("userId","roles","lockVersion")),
        Map.entry("REQUIREMENT",Set.of("requirementId","title","description","level","kind","priority","source","rationale","acceptanceCriteria","status","assigneeId","tagIds","currentVersionId","lockVersion","isWithdrawn","withdrawnBy","withdrawnAt","explanation")),
        Map.entry("REVIEW",Set.of("reviewId","requirementId","roundNo","snapshotTitle","snapshotDescription","snapshotLevel","snapshotKind","snapshotPriority","snapshotSource","snapshotRationale","snapshotAcceptanceCriteria","submittedBy","submittedAt","reviewStatus","reviewerId","decision","comment","decidedAt","status","firstSubmittedAt","currentVersionId","lockVersion","versionId")),
        Map.entry("VERSION",Set.of("versionId","requirementId","versionNo","initialReviewId","appliedChangeRequestId","changeReason")),
        Map.entry("RELATION",Set.of("relationId","sourceRequirementId","targetRequirementId","relationType","description","createdBy","createdAt")),
        Map.entry("CHANGE_REQUEST",Set.of("changeRequestId","requirementId","baseVersionId","requestTitle","reason","proposedTitle","proposedDescription","proposedLevel","proposedKind","proposedPriority","proposedSource","proposedRationale","proposedAcceptanceCriteria","status","createdBy","createdAt","updatedAt","appliedBy","appliedAt","lockVersion","requirementStatus","currentVersionId","requirementLockVersion","versionId","versionNo","changeReviewId")),
        Map.entry("CHANGE_REVIEW",Set.of("changeReviewId","requirementId","changeRequestId","roundNo","snapshotBaseVersionId","snapshotRequestTitle","snapshotReason","snapshotProposedTitle","snapshotProposedDescription","snapshotProposedLevel","snapshotProposedKind","snapshotProposedPriority","snapshotProposedSource","snapshotProposedRationale","snapshotProposedAcceptanceCriteria","submittedBy","submittedAt","reviewStatus","reviewerId","decision","comment","decidedAt","status","lockVersion")),
        Map.entry("TAG_ASSIGNMENT",Set.of("requirementId","tagIds")),Map.entry("TAG",Set.of("tagId","name","description")),
        Map.entry("COMMENT",Set.of("commentId","requirementId","content","isDeleted")));
    public AuditChange {
        if(!FIELDS.containsKey(targetType) || targetId==null || targetId.isBlank() || targetId.length()>128 || action==null || !action.matches("[A-Z][A-Z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid audit projection");
        before=safe(targetType,before);after=safe(targetType,after);
    }
    private static Map<String,Object> safe(String type,Map<String,Object> source) {
        if(source==null)return null;
        Map<String,Object> copy=new LinkedHashMap<>();
        for(var entry:source.entrySet()) {
            if(!FIELDS.get(type).contains(entry.getKey()))throw new IllegalArgumentException("Unsafe audit projection");
            Object value=entry.getValue();
            if(value instanceof List<?> list) { if(list.stream().anyMatch(v->!(v instanceof String || v instanceof Number || v instanceof Enum<?>)))throw new IllegalArgumentException("Unsafe audit projection");value=List.copyOf(list); }
            else if(value!=null && !(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>))throw new IllegalArgumentException("Unsafe audit projection");
            copy.put(entry.getKey(),value);
        }
        return Collections.unmodifiableMap(copy);
    }
}


package com.example.rms.shared.application;
/** Owner-provided PK-to-parent scalar lookup inside the independent failure-audit transaction.
 * No business content, mutation, foreign entity, row lock or authorization bypass. */
public interface FailureTargetContext {
    boolean supports(String targetType);
    Long requirementContext(String targetType,long targetId);
}

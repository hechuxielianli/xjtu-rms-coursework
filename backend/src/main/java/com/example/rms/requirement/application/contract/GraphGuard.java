package com.example.rms.requirement.application.contract;
/** Requirement-owned read port; relation owns its implementation and persistence. */
public interface GraphGuard { boolean hasActiveRelation(long requirementId); }

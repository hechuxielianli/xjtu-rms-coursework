package com.example.rms.shared.domain;
/** Cleanup after a committed transaction is unknown to the caller; never replay or audit it as rollback. */
public final class GraphOutcomeException extends RuntimeException {
    private final boolean committed;
    public GraphOutcomeException(boolean committed,Throwable cause) { super("GRAPH_OPERATION_OUTCOME_REQUIRES_REFRESH",cause);this.committed=committed; }
    public boolean committed() { return committed; }
}

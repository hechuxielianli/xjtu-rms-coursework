package com.example.rms.audit.application.contract;

import com.example.rms.audit.application.FailureAuditTransaction;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** REG-AUDIT-001: the observer does not own the business transaction or rewrite its outcome. */
class FailureObservationRegressionTest {
    private final Actor actor=new Actor(4,"Trusted","ENABLED",Set.of(RoleCode.REQUIREMENT_ENGINEER));
    private final FailureAuditTransaction transaction=mock(FailureAuditTransaction.class);
    private final FailureAuditService observer=new FailureAuditService(transaction);
    @Test void successfulCommandDoesNotCreateFailureAuditOrOpenTransaction() {
        Object result=new Object();
        assertSame(result,observer.attempt(actor,null,"REQUIREMENT","7","REQUIREMENT_REOPEN",trusted->{
            assertSame(actor,trusted);assertFalse(TransactionSynchronizationManager.isActualTransactionActive());return result;
        }));
        verifyNoInteractions(transaction);
    }
    @Test void forbiddenIsDeniedAndReturnsOriginalException() {
        var failure=new RmsException(ErrorCode.SELF_REVIEW);
        assertSame(failure,assertThrows(RmsException.class,()->observer.attempt(actor,7L,"CHANGE_REVIEW","8","CHANGE_REVIEW_DECISION",trusted->{throw failure;})));
        var change=ArgumentCaptor.forClass(AuditChange.class);
        verify(transaction).append(eq(actor),change.capture(),eq("DENIED"));
        assertEquals(7L,change.getValue().requirementId());assertEquals("8",change.getValue().targetId());
        assertNull(change.getValue().before());assertNull(change.getValue().after());
    }
    @Test void badInputConflictAndInternalFailureAreFailedWithNormalizedTargets() {
        for(ErrorCode code:List.of(ErrorCode.INVALID_INPUT,ErrorCode.STATE_CONFLICT,ErrorCode.INTERNAL_ERROR)) {
            reset(transaction);var failure=new RmsException(code);
            assertSame(failure,assertThrows(RmsException.class,()->observer.attempt(actor,null,"REQUIREMENT","untrusted-value","REQUIREMENT_REOPEN",trusted->{throw failure;})));
            var change=ArgumentCaptor.forClass(AuditChange.class);
            verify(transaction).append(eq(actor),change.capture(),eq("FAILED"));assertEquals("NEW",change.getValue().targetId());
        }
    }
    @Test void committedGraphCleanupNeverProducesFalseFailureAudit() {
        var failure=new GraphOutcomeException(true,new IllegalStateException("cleanup failed"));
        assertSame(failure,assertThrows(GraphOutcomeException.class,()->observer.attempt(actor,null,"RELATION","7","RELATION_REMOVE",trusted->{throw failure;})));
        verifyNoInteractions(transaction);
    }
    @Test void uncommittedGraphFailureIsStillFailed() {
        var failure=new GraphOutcomeException(false,new IllegalStateException("work failed"));
        assertSame(failure,assertThrows(GraphOutcomeException.class,()->observer.attempt(actor,null,"RELATION","7","RELATION_REMOVE",trusted->{throw failure;})));
        verify(transaction).append(eq(actor),any(),eq("FAILED"));
    }
    @Test void independentAuditFailureCannotReplaceOriginalBusinessError() {
        var failure=new RmsException(ErrorCode.STATE_CONFLICT);
        var auditFailure=new IllegalStateException("audit unavailable");
        doThrow(auditFailure).when(transaction).append(eq(actor),any(),eq("FAILED"));
        assertSame(failure,assertThrows(RmsException.class,()->observer.attempt(actor,null,"REQUIREMENT","7","REQUIREMENT_REOPEN",trusted->{throw failure;})));
        assertArrayEquals(new Throwable[]{auditFailure},failure.getSuppressed());
    }
}

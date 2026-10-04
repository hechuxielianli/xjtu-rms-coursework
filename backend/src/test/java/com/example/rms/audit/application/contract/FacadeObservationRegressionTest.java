package com.example.rms.audit.application.contract;

import com.example.rms.audit.application.FailureAuditTransaction;
import com.example.rms.change.application.*;
import com.example.rms.comment.application.*;
import com.example.rms.relation.application.*;
import com.example.rms.requirement.application.*;
import com.example.rms.review.application.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import com.example.rms.user.application.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the six real facade call sites, not just the extracted helper in isolation. */
class FacadeObservationRegressionTest {
    private record Attempt(String type,String action,Runnable call) {}
    private final Actor actor=new Actor(4,"Trusted","ENABLED",Set.of(RoleCode.values()));
    private final CurrentActor current=mock(CurrentActor.class);
    private final FailureAuditTransaction transaction=mock(FailureAuditTransaction.class);
    private final FailureAuditService observer=new FailureAuditService(transaction);
    private final GraphWriteExecutor graph=new GraphWriteExecutor() {
        public <T>T execute(Work<T> work) {
            try { return work.run(); }
            catch(RuntimeException failure) { throw failure; }
            catch(Exception failure) { throw new IllegalStateException(failure); }
        }
        public void requireActive() {}
    };
    private List<Attempt> attempts(RuntimeException failure) {
        when(current.require()).thenReturn(actor);
        var requirement=mock(RequirementTransactions.class);
        when(requirement.reopen(actor,7L,"0")).thenThrow(failure);
        var requirementService=new RequirementService(null,requirement,null,current,observer,graph);
        var review=mock(ReviewTransactions.class);
        when(review.submit(actor,7L,"0")).thenThrow(failure);
        var reviewService=new ReviewService(null,review,null,current,observer);
        var change=mock(ChangeTransactions.class);
        when(change.cancel(actor,7L,"0")).thenThrow(failure);
        var changeService=new ChangeService(null,change,null,current,observer);
        var comment=mock(CommentTransactions.class);
        doThrow(failure).when(comment).delete(actor,7L);
        var commentService=new CommentService(null,comment,null,current,observer);
        var relation=mock(RelationTransactions.class);
        doThrow(failure).when(relation).remove(actor,7L,"0","0");
        var relationService=new RelationService(null,relation,null,graph,current,observer);
        var user=mock(UserTransactions.class);
        when(user.enabled(actor,7L,"0",false)).thenThrow(failure);
        var userService=new UserAdministrationService(user,null,current,observer);
        return List.of(
            new Attempt("REQUIREMENT","REQUIREMENT_REOPEN",()->requirementService.reopen("7","0")),
            new Attempt("REVIEW","REQUIREMENT_SUBMIT",()->reviewService.submit("7","0")),
            new Attempt("CHANGE_REQUEST","CHANGE_CANCEL",()->changeService.cancel("7","0")),
            new Attempt("COMMENT","COMMENT_DELETE",()->commentService.delete("7")),
            new Attempt("RELATION","RELATION_REMOVE",()->relationService.remove("7","0","0")),
            new Attempt("USER","USER_DISABLE",()->userService.enabled("7","0",false)));
    }
    @Test void allSixFacadePathsUseIdenticalDeniedAndFailedClassification() {
        for(ErrorCode code:List.of(ErrorCode.FORBIDDEN,ErrorCode.STATE_CONFLICT)) {
            RuntimeException failure=new RmsException(code);
            for(Attempt attempt:attempts(failure)) {
                reset(transaction);
                assertSame(failure,assertThrows(RmsException.class,attempt.call()::run));
                var change=ArgumentCaptor.forClass(AuditChange.class);
                verify(transaction,times(1)).append(eq(actor),change.capture(),eq(code.status()==403?"DENIED":"FAILED"));
                // The review facade correctly identifies a submit target as a REQUIREMENT.
                assertEquals(attempt.type().equals("REVIEW")?"REQUIREMENT":attempt.type(),change.getValue().targetType());
                assertEquals(attempt.action(),change.getValue().action());assertEquals("7",change.getValue().targetId());
            }
        }
    }
    @Test void bothGraphFacadePathsSkipAuditAfterCommittedCleanupFailure() {
        when(current.require()).thenReturn(actor);
        var failure=new GraphOutcomeException(true,new IllegalStateException("cleanup failed"));
        var requirement=mock(RequirementTransactions.class);
        when(requirement.withdraw(actor,7L,"0")).thenThrow(failure);
        var requirementService=new RequirementService(null,requirement,null,current,observer,graph);
        assertSame(failure,assertThrows(GraphOutcomeException.class,()->requirementService.withdraw("7","0")));
        var relation=mock(RelationTransactions.class);
        doThrow(failure).when(relation).remove(actor,7L,"0","0");
        var relationService=new RelationService(null,relation,null,graph,current,observer);
        assertSame(failure,assertThrows(GraphOutcomeException.class,()->relationService.remove("7","0","0")));
        verifyNoInteractions(transaction);
    }
}

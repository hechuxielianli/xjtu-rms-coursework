package com.example.rms.audit.application.contract;
import com.example.rms.audit.application.FailureAuditTransaction;
import com.example.rms.audit.application.contract.AuditChange;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.GraphOutcomeException;
import com.example.rms.shared.domain.RmsException;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
/** Call only after the failed outer proxy/transaction has returned; actor is a captured trusted projection. */
@Service
public class FailureAuditService {
    private final FailureAuditTransaction transaction;
    public FailureAuditService(FailureAuditTransaction transaction) { this.transaction=transaction; }
    public void denied(Actor trustedActor,AuditChange change) { append(trustedActor,change,"DENIED"); }
    public void failed(Actor trustedActor,AuditChange change) { append(trustedActor,change,"FAILED"); }
    /** Observe a synchronous owner command. This helper opens no transaction around the proxied write. */
    public <T>T attempt(Actor trustedActor,Long requirementId,String targetType,String targetId,String action,Function<Actor,T> command) {
        try {
            return command.apply(trustedActor);
        } catch(RuntimeException failure) {
            // Cleanup can fail after a graph write has committed; its SUCCESS must not become FAILED.
            if(failure instanceof GraphOutcomeException graph && graph.committed())throw failure;
            String safeTarget=targetId!=null && targetId.matches("[1-9][0-9]{0,18}")?targetId:"NEW";
            try {
                AuditChange change=new AuditChange(requirementId,targetType,safeTarget,action,null,null);
                if(failure instanceof RmsException rms && rms.code().status()==403)denied(trustedActor,change);
                else failed(trustedActor,change);
            } catch(RuntimeException observationFailure) {
                // Keep the original business error if the independent observation itself cannot be saved.
                if(observationFailure!=failure)failure.addSuppressed(observationFailure);
            }
            throw failure;
        }
    }
    private void append(Actor actor,AuditChange change,String outcome) {
        if(actor==null || TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("FAILURE_AUDIT_REQUIRES_COMPLETED_OUTER_TRANSACTION");
        transaction.append(actor,change,outcome);
    }
}


package com.example.rms.audit.application;
import com.example.rms.audit.application.contract.AuditChange;
import com.example.rms.audit.application.port.AuditAppendPort;
import com.example.rms.audit.application.port.AuditQueryPort;
import com.example.rms.audit.domain.*;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.application.FailureTargetContext;
import java.util.List;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class FailureAuditTransaction {
    private final AuditAppendPort append;
    private final Clock clock;
    private final AuditQueryPort queries;private final List<FailureTargetContext> owners;
    public FailureAuditTransaction(AuditAppendPort append,Clock clock,AuditQueryPort queries,List<FailureTargetContext> owners) { this.append=append;this.clock=clock;this.queries=queries;this.owners=List.copyOf(owners); }
    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public void append(Actor trustedActor,AuditChange c,String outcome) {
        Long candidate=c.requirementId(),target=AuditChange.requirementCandidate(c.targetId());
        if(candidate==null && target!=null) {
            if(c.targetType().equals("REQUIREMENT") || c.targetType().equals("TAG_ASSIGNMENT"))candidate=target;
            else for(var owner:owners)if(owner.supports(c.targetType())) { candidate=owner.requirementContext(c.targetType(),target);break; }
        }
        // Missing/invalid locators remain system-only; never forge an FK or replace the business error.
        Long requirement=candidate!=null && candidate>0 && queries.requirementExists(candidate)?candidate:null;
        append.append(new AuditEntry(trustedActor.userId(),requirement,c.targetType(),c.targetId(),c.action(),outcome,c.before(),c.after(),clock.instant().truncatedTo(ChronoUnit.MICROS)));
    }
}


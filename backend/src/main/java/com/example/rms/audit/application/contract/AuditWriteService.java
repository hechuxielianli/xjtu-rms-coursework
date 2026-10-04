package com.example.rms.audit.application.contract;
import com.example.rms.audit.application.port.AuditAppendPort;
import com.example.rms.audit.domain.*;
import com.example.rms.shared.application.CurrentActor;
import com.example.rms.shared.application.Actor;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class AuditWriteService {
    private final AuditAppendPort append;
    private final CurrentActor current;
    private final Clock clock;
    public AuditWriteService(AuditAppendPort append,CurrentActor current,Clock clock) { this.append=append;this.current=current;this.clock=clock; }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void success(AuditChange c) { append.append(new AuditEntry(current.require().userId(),c.requirementId(),c.targetType(),c.targetId(),c.action(),"SUCCESS",c.before(),c.after(),clock.instant().truncatedTo(ChronoUnit.MICROS))); }
    /** Captured at the live Application entry before mutation, including a legitimate self-disable. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void success(Actor trustedActor,AuditChange c) { append.append(new AuditEntry(trustedActor.userId(),c.requirementId(),c.targetType(),c.targetId(),c.action(),"SUCCESS",c.before(),c.after(),clock.instant().truncatedTo(ChronoUnit.MICROS))); }
}

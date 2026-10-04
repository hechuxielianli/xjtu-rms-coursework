package com.example.rms.relation.infrastructure;
import com.example.rms.requirement.application.contract.GraphGuard;
import com.example.rms.relation.application.port.RelationStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component
public class RelationGraphGuard implements GraphGuard {
    private final RelationStore store;public RelationGraphGuard(RelationStore store){this.store=store;}
    @Override @Transactional(propagation=Propagation.MANDATORY,readOnly=true) public boolean hasActiveRelation(long id){return store.incident(id);}
}

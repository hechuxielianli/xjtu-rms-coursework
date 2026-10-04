package com.example.rms.change.application;
import com.example.rms.change.application.port.ChangeStore;
import com.example.rms.requirement.application.contract.RequirementMutationService;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ChangeService {
    private final ChangeStore store;private final ChangeTransactions writes;private final RequirementMutationService requirements;private final CurrentActor current;private final FailureAuditService failures;
    public ChangeService(ChangeStore store,ChangeTransactions writes,RequirementMutationService requirements,CurrentActor current,FailureAuditService failures) { this.store=store;this.writes=writes;this.requirements=requirements;this.current=current;this.failures=failures; }
    public ChangeData create(String id,String title,String reason,String expected) { return attempt(AuditChange.requirementCandidate(id),"CHANGE_REQUEST","NEW","CHANGE_CREATE",a->writes.create(a,InputPolicy.decimalId(id),title,reason,expected)); }
    public ChangeData edit(String id,Map<String,Object> fields) { return attempt("CHANGE_REQUEST",id,"CHANGE_EDIT",a->writes.edit(a,InputPolicy.decimalId(id),fields)); }
    public ChangeReviewData submit(String id,String expected) { return attempt("CHANGE_REQUEST",id,"CHANGE_SUBMIT",a->writes.submit(a,InputPolicy.decimalId(id),expected)); }
    public ChangeData cancel(String id,String expected) { return attempt("CHANGE_REQUEST",id,"CHANGE_CANCEL",a->writes.cancel(a,InputPolicy.decimalId(id),expected)); }
    public ChangeTransactions.DecisionResult decide(String id,String decision,String comment,String expected) { return attempt("CHANGE_REVIEW",id,"CHANGE_REVIEW_DECISION",a->writes.decide(a,InputPolicy.decimalId(id),decision,comment,expected)); }
    public ChangeTransactions.ApplyResult apply(String id,String expectedRequirement,String expectedChange) { return attempt("CHANGE_REQUEST",id,"CHANGE_APPLY",a->writes.apply(a,InputPolicy.decimalId(id),expectedRequirement,expectedChange)); }
    @Transactional(readOnly=true) public ChangeData read(String id) { readActor();return store.read(InputPolicy.decimalId(id)); }
    @Transactional(readOnly=true) public PageData<ChangeData> list(String id,String status,int page,int size) { readActor();long parent=InputPolicy.decimalId(id);var paging=new Paging(page,size);if(status!=null)InputPolicy.oneOf(status,"DRAFT","UNDER_REVIEW","APPROVED","REJECTED","APPLIED","CANCELLED");requirements.readContext(parent);return store.list(parent,status,paging); }
    @Transactional(readOnly=true) public PageData<ChangeReviewData> history(String id,int page,int size) { readActor();long change=InputPolicy.decimalId(id);var paging=new Paging(page,size);store.read(change);return store.reviews(change,paging); }
    @Transactional(readOnly=true) public PageData<ChangeReviewData> pending(int page,int size) { current.require().requireAny(RoleCode.REVIEWER);return store.reviews(null,new Paging(page,size)); }
    private void readActor() { current.require().requireAny(RoleCode.values()); }
    private <T>T attempt(String type,String target,String action,Function<Actor,T> call) { return attempt(null,type,target,action,call); }
    private <T>T attempt(Long parent,String type,String target,String action,Function<Actor,T> call) {
        return failures.attempt(current.require(),parent,type,target,action,call);
    }
}

package com.example.rms.review.application;
import com.example.rms.review.application.port.ReviewStore;
import com.example.rms.requirement.application.contract.RequirementMutationService;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReviewService {
    private final ReviewStore store;private final ReviewTransactions writes;private final RequirementMutationService requirements;private final CurrentActor current;private final FailureAuditService failures;
    public ReviewService(ReviewStore store,ReviewTransactions writes,RequirementMutationService requirements,CurrentActor current,FailureAuditService failures) { this.store=store;this.writes=writes;this.requirements=requirements;this.current=current;this.failures=failures; }
    public ReviewData submit(String id,String expected) { return attempt("REQUIREMENT",id,"REQUIREMENT_SUBMIT",a->writes.submit(a,InputPolicy.decimalId(id),expected)); }
    public ReviewTransactions.DecisionResult decide(String id,String decision,String comment,String expected) { return attempt("REVIEW",id,"REQUIREMENT_REVIEW_DECISION",a->writes.decide(a,InputPolicy.decimalId(id),decision,comment,expected)); }
    @Transactional(readOnly=true) public PageData<ReviewData> pending(int page,int size) { current.require().requireAny(RoleCode.REVIEWER);return store.list(null,new Paging(page,size)); }
    @Transactional(readOnly=true) public PageData<ReviewData> history(String id,int page,int size) { current.require().requireAny(RoleCode.values());long requirement=InputPolicy.decimalId(id);var paging=new Paging(page,size);requirements.readContext(requirement);return store.list(requirement,paging); }
    private <T>T attempt(String type,String target,String action,Function<Actor,T> call) {
        return failures.attempt(current.require(),null,type,target,action,call);
    }
}

package com.example.rms.requirement.application;
import com.example.rms.requirement.application.port.RequirementStore;
import com.example.rms.user.application.contract.*;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RequirementService {
    public record Detail(RequirementData requirement,UserSummaryData creator,UserSummaryData assignee) {}
    private final RequirementStore store;private final RequirementTransactions writes;private final UserAccessService users;private final CurrentActor current;private final FailureAuditService failures;private final GraphWriteExecutor graph;
    public RequirementService(RequirementStore store,RequirementTransactions writes,UserAccessService users,CurrentActor current,FailureAuditService failures,GraphWriteExecutor graph) { this.store=store;this.writes=writes;this.users=users;this.current=current;this.failures=failures;this.graph=graph; }
    @Transactional(readOnly=true) public Detail read(String id) { readActor();return detail(store.read(InputPolicy.decimalId(id))); }
    @Transactional(readOnly=true) public PageData<Detail> list(int page,int size,String keyword,String status,String kind,String level,String priority,String tag) {
        readActor();if(status!=null)InputPolicy.oneOf(status,"DRAFT","UNDER_REVIEW","APPROVED","REJECTED","IMPLEMENTED","VERIFIED");if(kind!=null)InputPolicy.oneOf(kind,"FUNCTIONAL","QUALITY","CONSTRAINT");if(level!=null)InputPolicy.oneOf(level,"BUSINESS","USER","SYSTEM");if(priority!=null)InputPolicy.oneOf(priority,"LOW","MEDIUM","HIGH","CRITICAL");
        var data=store.list(new RequirementQuery(new Paging(page,size),keyword,status,kind,level,priority,tag==null?null:InputPolicy.decimalId(tag)));return new PageData<>(data.items().stream().map(this::detail).toList(),data.page(),data.size(),data.totalElements());
    }
    public Detail create(String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria) { return detail(attempt("REQUIREMENT","NEW","REQUIREMENT_CREATE",actor->writes.create(actor,title,description,level,kind,priority,source,rationale,acceptanceCriteria))); }
    public Detail content(String id,Map<String,Object> fields) { return detail(attempt("REQUIREMENT",id,"REQUIREMENT_CONTENT_EDIT",actor->writes.content(actor,InputPolicy.decimalId(id),fields))); }
    public Detail metadata(String id,Map<String,Object> fields) { return detail(attempt("REQUIREMENT",id,"REQUIREMENT_METADATA_EDIT",actor->writes.metadata(actor,InputPolicy.decimalId(id),fields))); }
    public Detail withdraw(String id,String expected) { return detail(attempt("REQUIREMENT",id,"REQUIREMENT_WITHDRAW",actor->graph.execute(()->writes.withdraw(actor,InputPolicy.decimalId(id),expected)))); }
    public Detail reopen(String id,String expected) { return detail(attempt("REQUIREMENT",id,"REQUIREMENT_REOPEN",actor->writes.reopen(actor,InputPolicy.decimalId(id),expected))); }
    public Detail confirm(String id,String version,String expected,String description,boolean verify) { return detail(attempt("REQUIREMENT",id,verify?"REQUIREMENT_VERIFY":"REQUIREMENT_IMPLEMENT",actor->writes.confirm(actor,InputPolicy.decimalId(id),version,expected,description,verify))); }
    @Transactional(readOnly=true) public PageData<TagData> tags(int page,int size,String name) { readActor();InputPolicy.nullableText(name,50);return store.tags(new Paging(page,size),name); }
    public TagData createTag(String name,String description) { return attempt("TAG","NEW","TAG_CREATE",actor->writes.createTag(actor,name,description)); }
    public TagData editTag(String id,Map<String,Object> fields) { return attempt("TAG",id,"TAG_EDIT",actor->writes.editTag(actor,InputPolicy.decimalId(id),fields)); }
    private Detail detail(RequirementData r) { return new Detail(r,users.readSummary(r.creatorId()),r.assigneeId()==null?null:users.readSummary(r.assigneeId())); }
    private void readActor() { current.require().requireAny(RoleCode.values()); }
    private <T>T attempt(String type,String target,String action,Function<Actor,T> call) {
        return failures.attempt(current.require(),null,type,target,action,call);
    }
}

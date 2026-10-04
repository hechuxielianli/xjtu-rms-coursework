package com.example.rms.requirement.application;
import com.example.rms.requirement.application.port.RequirementStore;
import com.example.rms.user.application.contract.UserAccessService;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.application.GraphWriteExecutor;
import com.example.rms.requirement.application.contract.GraphGuard;
import com.example.rms.shared.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class RequirementTransactions {
    private static final Set<String> CONTENT=Set.of("title","description","level","kind","priority","source","rationale","acceptanceCriteria","expectedLockVersion");
    private final RequirementStore store;private final UserAccessService users;private final AuditWriteService audit;private final Clock clock;private final GraphWriteExecutor graph;private final GraphGuard guard;
    public RequirementTransactions(RequirementStore store,UserAccessService users,AuditWriteService audit,Clock clock,GraphWriteExecutor graph,GraphGuard guard) { this.store=store;this.users=users;this.audit=audit;this.clock=clock;this.graph=graph;this.guard=guard; }
    @Transactional(rollbackFor=Exception.class)
    public RequirementData create(Actor actor,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria) {
        actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);validate(title,description,level,kind,priority);
        String key="REQ-"+UUID.randomUUID().toString().replace("-","").substring(0,28);
        RequirementData after=store.create(key,title,description,level,kind,priority,source,rationale,acceptanceCriteria,actor.userId(),now());success(actor,"REQUIREMENT_CREATE",null,after);return after;
    }
    @Transactional(rollbackFor=Exception.class)
    public RequirementData content(Actor actor,long id,Map<String,Object> fields) {
        actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);var patch=new InputPatch(fields,CONTENT,"expectedLockVersion");var before=store.lock(id);active(before);if(!before.status().equals("DRAFT"))throw new RmsException(ErrorCode.STATE_CONFLICT);InputPolicy.expected(InputPolicy.lockVersion(patch.string("expectedLockVersion",false)),before.lockVersion());
        String title=patch.has("title")?patch.string("title",false):before.title(),description=patch.has("description")?patch.string("description",false):before.description(),level=patch.has("level")?patch.string("level",false):before.level(),kind=patch.has("kind")?patch.string("kind",false):before.kind(),priority=patch.has("priority")?patch.string("priority",false):before.priority();validate(title,description,level,kind,priority);
        var after=store.content(id,title,description,level,kind,priority,patch.has("source")?patch.string("source",true):before.source(),patch.has("rationale")?patch.string("rationale",true):before.rationale(),patch.has("acceptanceCriteria")?patch.string("acceptanceCriteria",true):before.acceptanceCriteria(),now());success(actor,"REQUIREMENT_CONTENT_EDIT",before,after);return after;
    }
    @Transactional(rollbackFor=Exception.class)
    public RequirementData metadata(Actor actor,long id,Map<String,Object> fields) {
        actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);var patch=new InputPatch(fields,Set.of("assigneeId","tagIds","expectedLockVersion"),"expectedLockVersion");var before=store.lock(id);active(before);InputPolicy.expected(InputPolicy.lockVersion(patch.string("expectedLockVersion",false)),before.lockVersion());
        Long assignee=before.assigneeId();if(patch.has("assigneeId")){String raw=patch.string("assigneeId",true);assignee=raw==null?null:InputPolicy.decimalId(raw);if(assignee!=null)users.requireEnabledAssignee(assignee);}
        List<Long> tags=patch.has("tagIds")?patch.strings("tagIds").stream().map(InputPolicy::decimalId).toList():null;
        var after=store.metadata(id,assignee,tags,actor.userId(),now());success(actor,"REQUIREMENT_METADATA_EDIT",before,after);return after;
    }
    @Transactional(rollbackFor=Exception.class)
    public TagData createTag(Actor actor,String name,String description) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);var after=store.createTag(tagName(name),InputPolicy.nullableText(description,500),actor.userId(),now());audit.success(actor,new AuditChange(null,"TAG",Long.toString(after.tagId()),"TAG_CREATE",null,tagSafe(after)));return after; }
    @Transactional(rollbackFor=Exception.class)
    public TagData editTag(Actor actor,long id,Map<String,Object> fields) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);var patch=new InputPatch(fields,Set.of("name","description"));var before=store.tag(id,true);var after=store.editTag(id,patch.has("name")?tagName(patch.string("name",false)):before.name(),patch.has("description")?InputPolicy.nullableText(patch.string("description",true),500):before.description());audit.success(actor,new AuditChange(null,"TAG",Long.toString(id),"TAG_EDIT",tagSafe(before),tagSafe(after)));return after; }
    @Transactional(rollbackFor=Exception.class)
    public RequirementData reopen(Actor actor,long id,String expected) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);long revision=InputPolicy.lockVersion(expected);var before=store.lock(id);active(before);InputPolicy.expected(revision,before.lockVersion());if(!before.status().equals("REJECTED"))throw new RmsException(ErrorCode.STATE_CONFLICT);var after=store.reopen(id,now());success(actor,"REQUIREMENT_REOPEN",before,after);return after; }
    @Transactional(rollbackFor=Exception.class)
    public RequirementData confirm(Actor actor,long id,String expectedVersion,String expected,String description,boolean verify) { actor.requireAny(RoleCode.PROJECT_MEMBER);long version=InputPolicy.decimalId(expectedVersion),revision=InputPolicy.lockVersion(expected);InputPolicy.nonBlank(description,Integer.MAX_VALUE);var before=store.lock(id);active(before);InputPolicy.expected(revision,before.lockVersion());if(!before.status().equals(verify?"IMPLEMENTED":"APPROVED"))throw new RmsException(ErrorCode.STATE_CONFLICT);if(before.currentVersionId()==null || before.currentVersionId()!=version)throw new RmsException(ErrorCode.BASE_VERSION_STALE);var after=store.confirm(id,verify,now());var values=safe(after);values.put("explanation",description);audit.success(actor,new AuditChange(id,"REQUIREMENT",Long.toString(id),verify?"REQUIREMENT_VERIFY":"REQUIREMENT_IMPLEMENT",safe(before),values));return after; }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public RequirementData withdraw(Actor actor,long id,String expected) { graph.requireActive();actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);long revision=InputPolicy.lockVersion(expected);var before=store.lock(id);active(before);InputPolicy.expected(revision,before.lockVersion());if(!"DRAFT".equals(before.status()) || before.firstSubmittedAt()!=null || guard.hasActiveRelation(id))throw new RmsException(ErrorCode.STATE_CONFLICT);var after=store.withdrawn(id,actor.userId(),now());var oldValues=safe(before);oldValues.put("isWithdrawn",before.isWithdrawn());oldValues.put("withdrawnBy",before.withdrawnBy());oldValues.put("withdrawnAt",before.withdrawnAt()==null?null:before.withdrawnAt().toString());var newValues=safe(after);newValues.put("isWithdrawn",after.isWithdrawn());newValues.put("withdrawnBy",after.withdrawnBy());newValues.put("withdrawnAt",after.withdrawnAt().toString());audit.success(actor,new AuditChange(id,"REQUIREMENT",Long.toString(id),"REQUIREMENT_WITHDRAW",oldValues,newValues));return after; }
    private static String tagName(String raw) { InputPolicy.nonBlank(raw,50);return InputPolicy.nonBlank(raw.trim(),50); }
    private static void validate(String title,String description,String level,String kind,String priority) { InputPolicy.nonBlank(title,200);InputPolicy.nonBlank(description,Integer.MAX_VALUE);InputPolicy.oneOf(level,"BUSINESS","USER","SYSTEM");InputPolicy.oneOf(kind,"FUNCTIONAL","QUALITY","CONSTRAINT");InputPolicy.oneOf(priority,"LOW","MEDIUM","HIGH","CRITICAL"); }
    private static void active(RequirementData r) { if(r.isWithdrawn()!=0)throw new RmsException(ErrorCode.STATE_CONFLICT); }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static Map<String,Object> safe(RequirementData r) { Map<String,Object> values=new LinkedHashMap<>();values.put("requirementId",r.requirementId());values.put("title",r.title());values.put("description",r.description());values.put("level",r.level());values.put("kind",r.kind());values.put("priority",r.priority());values.put("source",r.source());values.put("rationale",r.rationale());values.put("acceptanceCriteria",r.acceptanceCriteria());values.put("status",r.status());values.put("assigneeId",r.assigneeId());values.put("tagIds",r.tags().stream().map(TagData::tagId).toList());values.put("currentVersionId",r.currentVersionId());values.put("lockVersion",r.lockVersion());return values; }
    private static Map<String,Object> tagSafe(TagData t) { Map<String,Object> result=new LinkedHashMap<>();result.put("tagId",t.tagId());result.put("name",t.name());result.put("description",t.description());return result; }
    private void success(Actor actor,String action,RequirementData before,RequirementData after) { audit.success(actor,new AuditChange(after.requirementId(),"REQUIREMENT",Long.toString(after.requirementId()),action,before==null?null:safe(before),safe(after))); }
}

package com.example.rms.relation.application;
import com.example.rms.relation.application.port.RelationStore;
import com.example.rms.relation.domain.RelationType;
import com.example.rms.requirement.application.contract.*;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class RelationTransactions {
    private final RelationStore store;private final RequirementMutationService requirements;private final GraphWriteExecutor graph;private final AuditWriteService audit;private final Clock clock;
    public RelationTransactions(RelationStore store,RequirementMutationService requirements,GraphWriteExecutor graph,AuditWriteService audit,Clock clock){this.store=store;this.requirements=requirements;this.graph=graph;this.audit=audit;this.clock=clock;}
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public RelationData create(Actor actor,long source,long target,String type,String description,String expectedSource,String expectedTarget){graph.requireActive();actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);var relationType=RelationType.parse(type);InputPolicy.nullableText(description,1000);long sourceRevision=InputPolicy.lockVersion(expectedSource),targetRevision=InputPolicy.lockVersion(expectedTarget);if(source==target)throw new RmsException(ErrorCode.INVALID_INPUT);lockParents(source,target,sourceRevision,targetRevision);long storedSource=source,storedTarget=target;if(relationType.symmetric() && source>target){storedSource=target;storedTarget=source;}if(store.exists(storedSource,storedTarget,type))throw new RmsException(ErrorCode.DUPLICATE);if(relationType==RelationType.DEPENDS_ON && reachable(target,source))throw new RmsException(ErrorCode.DEPENDENCY_CYCLE);var after=store.create(storedSource,storedTarget,type,description,actor.userId(),clock.instant().truncatedTo(ChronoUnit.MICROS));audit.success(actor,new AuditChange(storedSource,"RELATION",Long.toString(after.relationId()),"RELATION_CREATE",null,safe(after)));return after;}
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void remove(Actor actor,long id,String expectedSource,String expectedTarget){graph.requireActive();actor.requireAny(RoleCode.REQUIREMENT_ENGINEER);long sourceRevision=InputPolicy.lockVersion(expectedSource),targetRevision=InputPolicy.lockVersion(expectedTarget);var before=store.read(id);lockParents(before.sourceRequirementId(),before.targetRequirementId(),sourceRevision,targetRevision);before=store.lock(id);store.remove(id);audit.success(actor,new AuditChange(before.sourceRequirementId(),"RELATION",Long.toString(id),"RELATION_REMOVE",safe(before),null));}
    private void lockParents(long source,long target,long sourceRevision,long targetRevision){GraphRequirement first=requirements.lockForGraph(Math.min(source,target)),second=requirements.lockForGraph(Math.max(source,target));GraphRequirement sourceContext=source<target?first:second,targetContext=source<target?second:first;InputPolicy.expected(sourceRevision,sourceContext.lockVersion());InputPolicy.expected(targetRevision,targetContext.lockVersion());}
    private boolean reachable(long start,long destination){var visited=new HashSet<Long>();var queue=new ArrayDeque<Long>();queue.add(start);while(!queue.isEmpty()){long id=queue.removeFirst();if(id==destination)return true;if(visited.add(id))queue.addAll(store.outgoingDependencies(id));}return false;}
    private static Map<String,Object> safe(RelationData r){var m=new LinkedHashMap<String,Object>();m.put("relationId",r.relationId());m.put("sourceRequirementId",r.sourceRequirementId());m.put("targetRequirementId",r.targetRequirementId());m.put("relationType",r.relationType());m.put("description",r.description());m.put("createdBy",r.createdBy());m.put("createdAt",r.createdAt().toString());return m;}
}

package com.example.rms.relation.application;
import com.example.rms.relation.application.port.RelationStore;
import com.example.rms.relation.domain.RelationType;
import com.example.rms.requirement.application.contract.*;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RelationService {
    public record Trace(long rootRequirementId,List<RequirementView> nodes,List<RelationData> edges,int depth,boolean truncated){}
    private final RelationStore store;private final RelationTransactions writes;private final RequirementMutationService requirements;private final GraphWriteExecutor graph;private final CurrentActor current;private final FailureAuditService failures;
    public RelationService(RelationStore store,RelationTransactions writes,RequirementMutationService requirements,GraphWriteExecutor graph,CurrentActor current,FailureAuditService failures){this.store=store;this.writes=writes;this.requirements=requirements;this.graph=graph;this.current=current;this.failures=failures;}
    public RelationData create(String source,String target,String type,String description,String expectedSource,String expectedTarget){return attempt(AuditChange.requirementCandidate(source),"NEW","RELATION_CREATE",a->graph.execute(()->writes.create(a,InputPolicy.decimalId(source),InputPolicy.decimalId(target),type,description,expectedSource,expectedTarget)));}
    public void remove(String id,String source,String target){attempt(id,"RELATION_REMOVE",a->graph.execute(()->{writes.remove(a,InputPolicy.decimalId(id),source,target);return null;}));}
    @Transactional(readOnly=true) public PageData<RelationData> list(String raw,int page,int size){readActor();long id=InputPolicy.decimalId(raw);requirements.readContext(id);return store.list(id,new Paging(page,size));}
    @Transactional(readOnly=true) public Trace trace(String raw,int depth,String type){readActor();long root=InputPolicy.decimalId(raw);if(depth<1 || depth>10)throw new RmsException(ErrorCode.INVALID_INPUT);if(type!=null)RelationType.parse(type);var nodes=new LinkedHashMap<Long,RequirementView>();var edges=new LinkedHashMap<Long,RelationData>();var distance=new HashMap<Long,Integer>();var queue=new ArrayDeque<Long>();nodes.put(root,requirements.readView(root));distance.put(root,0);queue.add(root);boolean truncated=false;while(!queue.isEmpty()){long id=queue.removeFirst();int d=distance.get(id);for(var edge:store.incident(id,type)){long other=edge.sourceRequirementId()==id?edge.targetRequirementId():edge.sourceRequirementId();if(d==depth){if(!edges.containsKey(edge.relationId()))truncated=true;continue;}if(!nodes.containsKey(other)){if(nodes.size()>=1000){truncated=true;continue;}nodes.put(other,requirements.readView(other));distance.put(other,d+1);queue.add(other);}if(edges.size()<5000 || edges.containsKey(edge.relationId()))edges.put(edge.relationId(),edge);else truncated=true;}}return new Trace(root,List.copyOf(nodes.values()),List.copyOf(edges.values()),depth,truncated);}
    private void readActor(){current.require().requireAny(RoleCode.values());}
    private <T>T attempt(String id,String action,Function<Actor,T> command){return attempt(null,id,action,command);}
    private <T>T attempt(Long parent,String id,String action,Function<Actor,T> command){
        return failures.attempt(current.require(),parent,"RELATION",id,action,command);
    }
}

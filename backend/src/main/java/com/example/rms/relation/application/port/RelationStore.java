package com.example.rms.relation.application.port;
import com.example.rms.relation.application.RelationData;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.Paging;
import java.time.Instant;
import java.util.List;
public interface RelationStore {
    RelationData read(long id);
    RelationData lock(long id);
    boolean exists(long source,long target,String type);
    boolean incident(long requirementId);
    List<Long> outgoingDependencies(long id);
    List<RelationData> incident(long id,String type);
    PageData<RelationData> list(long id,Paging paging);
    RelationData create(long source,long target,String type,String description,long actorId,Instant now);
    void remove(long id);
}

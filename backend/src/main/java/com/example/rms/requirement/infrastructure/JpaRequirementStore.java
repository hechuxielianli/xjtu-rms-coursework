package com.example.rms.requirement.infrastructure;
import com.example.rms.requirement.application.*;
import com.example.rms.requirement.application.port.RequirementStore;
import com.example.rms.requirement.infrastructure.persistence.*;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class JpaRequirementStore implements RequirementStore {
    private final EntityManager em;
    public JpaRequirementStore(EntityManager em) { this.em=em; }
    private RequirementEntity find(long id,boolean lock) { RequirementEntity r=lock?em.find(RequirementEntity.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(RequirementEntity.class,id);if(r==null)throw new RmsException(ErrorCode.NOT_FOUND);if(lock)em.refresh(r,LockModeType.PESSIMISTIC_WRITE);return r; }
    @Override public RequirementData read(long id) { return data(find(id,false)); }
    @Override public RequirementData lock(long id) { return data(find(id,true)); }
    @Override public RequirementData create(String key,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,long actorId,Instant now) { var r=RequirementEntity.draft(key,title,description,level,kind,priority,source,rationale,acceptanceCriteria,actorId,now);em.persist(r);em.flush();return data(r); }
    @Override public RequirementData content(long id,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,Instant now) { var r=find(id,false);r.editDraft(title,description,level,kind,priority,source,rationale,acceptanceCriteria,now);em.flush();return data(r); }
    @Override public RequirementData metadata(long id,Long assigneeId,List<Long> tagIds,long actorId,Instant now) {
        var r=find(id,false);r.assign(assigneeId,now);
        if(tagIds!=null) {
            for(long tagId:tagIds)tag(tagId,false);Set<Long> retained=new HashSet<>();
            for(var link:em.createQuery("select t from RequirementTagEntity t where t.requirementId=:id",RequirementTagEntity.class).setParameter("id",id).getResultList()) { if(!tagIds.contains(link.getTagId()))em.remove(link);else retained.add(link.getTagId()); }
            for(long tagId:tagIds)if(!retained.contains(tagId))em.persist(RequirementTagEntity.add(id,tagId,actorId,now));
        }
        em.flush();return data(r);
    }
    @Override public RequirementData submitted(long id,Instant now) { var r=find(id,false);r.submitted(now);em.flush();return data(r); }
    @Override public RequirementData reviewRejected(long id,boolean requestChanges,Instant now) { var r=find(id,false);r.reviewRejected(requestChanges,now);em.flush();return data(r); }
    @Override public RequirementData initialApproved(long id,long versionId,ContentSnapshot c,Instant now) { var r=find(id,false);r.initialApproved(versionId,c,now);em.flush();return data(r); }
    @Override public RequirementData appliedVersion(long id,long versionId,ContentSnapshot c,Instant now) { var r=find(id,false);r.appliedVersion(versionId,c,now);em.flush();return data(r); }
    @Override public RequirementData withdrawn(long id,long actorId,Instant now) { var r=find(id,false);r.withdraw(actorId,now);em.flush();return data(r); }
    @Override public RequirementData reopen(long id,Instant now) { var r=find(id,false);r.reopen(now);em.flush();return data(r); }
    @Override public RequirementData confirm(long id,boolean verify,Instant now) { var r=find(id,false);r.confirm(verify,now);em.flush();return data(r); }
    @Override public PageData<RequirementData> list(RequirementQuery f) {
        StringBuilder where=new StringBuilder(" where r.isWithdrawn=0");Map<String,Object> parameters=new LinkedHashMap<>();
        for(var entry:new String[][]{{"status",f.status()},{"kind",f.kind()},{"level",f.level()},{"priority",f.priority()}})if(entry[1]!=null){where.append(" and r.").append(entry[0]).append("=:").append(entry[0]);parameters.put(entry[0],entry[1]);}
        if(f.keyword()!=null){where.append(" and (r.requirementKey like :keyword escape '!' or r.title like :keyword escape '!' or r.description like :keyword escape '!')");parameters.put("keyword",like(f.keyword()));}
        if(f.tagId()!=null){where.append(" and exists (select rt.tagId from RequirementTagEntity rt where rt.requirementId=r.requirementId and rt.tagId=:tag)");parameters.put("tag",f.tagId());}
        var rows=em.createQuery("select r from RequirementEntity r"+where+" order by r.requirementId",RequirementEntity.class);var count=em.createQuery("select count(r) from RequirementEntity r"+where,Long.class);
        parameters.forEach((name,value)->{rows.setParameter(name,value);count.setParameter(name,value);});
        Paging p=f.paging();
        List<RequirementEntity> page=rows.setFirstResult(p.offset()).setMaxResults(p.size()).getResultList();
        Map<Long,List<TagData>> pageTags=pageTags(page);
        List<RequirementData> items=page.stream().map(row->data(row,pageTags.getOrDefault(row.getRequirementId(),List.of()))).toList();
        return new PageData<>(items,p.page(),p.size(),count.getSingleResult());
    }
    @Override public TagData tag(long id,boolean lock) { TagEntity tag=lock?em.find(TagEntity.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(TagEntity.class,id);if(tag==null)throw new RmsException(ErrorCode.NOT_FOUND);return data(tag); }
    @Override public PageData<TagData> tags(Paging p,String name) { String filter=name==null?"":" where t.name like :name escape '!'";var rows=em.createQuery("select t from TagEntity t"+filter+" order by t.tagId",TagEntity.class);var count=em.createQuery("select count(t) from TagEntity t"+filter,Long.class);if(name!=null){rows.setParameter("name",like(name));count.setParameter("name",like(name));}return new PageData<>(rows.setFirstResult(p.offset()).setMaxResults(p.size()).getResultList().stream().map(JpaRequirementStore::data).toList(),p.page(),p.size(),count.getSingleResult()); }
    @Override public TagData createTag(String name,String description,long actorId,Instant now) { var tag=TagEntity.create(name,description,actorId,now);em.persist(tag);em.flush();return data(tag); }
    @Override public TagData editTag(long id,String name,String description) { var tag=em.find(TagEntity.class,id);tag.edit(name,description);em.flush();return data(tag); }
    private Map<Long,List<TagData>> pageTags(List<RequirementEntity> page) {
        if(page.isEmpty())return Map.of();
        List<Long> ids=page.stream().map(RequirementEntity::getRequirementId).toList();
        List<Object[]> projections=em.createQuery("select rt.requirementId,t from TagEntity t,RequirementTagEntity rt where rt.requirementId in :ids and t.tagId=rt.tagId order by rt.requirementId,t.tagId",Object[].class).setParameter("ids",ids).getResultList();
        Map<Long,List<TagData>> grouped=new HashMap<>();
        for(Object[] projection:projections)grouped.computeIfAbsent((Long)projection[0],ignored->new ArrayList<>()).add(data((TagEntity)projection[1]));
        grouped.replaceAll((id,tags)->List.copyOf(tags));
        return grouped;
    }
    private RequirementData data(RequirementEntity r) {
        var tags=em.createQuery("select t from TagEntity t,RequirementTagEntity rt where rt.requirementId=:id and t.tagId=rt.tagId order by t.tagId",TagEntity.class).setParameter("id",r.getRequirementId()).getResultList().stream().map(JpaRequirementStore::data).toList();
        return data(r,tags);
    }
    /** Pure row assembly; collection callers supply their owner-local batch projection. */
    private static RequirementData data(RequirementEntity r,List<TagData> tags) { return new RequirementData(r.getRequirementId(),r.getRequirementKey(),r.getTitle(),r.getDescription(),r.getLevel(),r.getKind(),r.getPriority(),r.getSource(),r.getRationale(),r.getAcceptanceCriteria(),r.getStatus(),r.getCreatorId(),r.getAssigneeId(),r.getCurrentVersionId(),r.getFirstSubmittedAt(),r.getIsWithdrawn(),r.getWithdrawnBy(),r.getWithdrawnAt(),r.getCreatedAt(),r.getUpdatedAt(),r.getLockVersion(),tags); }
    private static TagData data(TagEntity t) { return new TagData(t.getTagId(),t.getName(),t.getDescription(),t.getCreatedBy(),t.getCreatedAt()); }
    private static String like(String value) { return "%"+value.replace("!","!!").replace("%","!%").replace("_","!_")+"%"; }
}

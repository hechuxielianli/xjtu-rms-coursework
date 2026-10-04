package com.example.rms.requirement.application.port;
import com.example.rms.requirement.application.*;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.Paging;
import java.time.Instant;
import java.util.List;
import com.example.rms.shared.domain.ContentSnapshot;
public interface RequirementStore {
    RequirementData read(long id);
    RequirementData lock(long id);
    PageData<RequirementData> list(RequirementQuery query);
    RequirementData create(String key,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,long actorId,Instant now);
    RequirementData content(long id,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,Instant now);
    RequirementData metadata(long id,Long assigneeId,List<Long> tagIds,long actorId,Instant now);
    RequirementData submitted(long id,Instant now);
    RequirementData reviewRejected(long id,boolean requestChanges,Instant now);
    RequirementData initialApproved(long id,long versionId,ContentSnapshot content,Instant now);
    RequirementData appliedVersion(long id,long versionId,ContentSnapshot content,Instant now);
    RequirementData withdrawn(long id,long actorId,Instant now);
    RequirementData reopen(long id,Instant now);
    RequirementData confirm(long id,boolean verify,Instant now);
    TagData tag(long id,boolean lock);
    PageData<TagData> tags(Paging paging,String name);
    TagData createTag(String name,String description,long actorId,Instant now);
    TagData editTag(long id,String name,String description);
}

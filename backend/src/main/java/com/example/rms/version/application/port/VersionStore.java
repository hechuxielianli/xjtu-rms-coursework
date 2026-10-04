package com.example.rms.version.application.port;
import com.example.rms.version.application.contract.VersionData;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import java.time.Instant;
public interface VersionStore {
    boolean requirementExists(long id);
    long count(long requirementId);
    VersionData appendInitial(long requirementId,long reviewId,ContentSnapshot content,long actorId,Instant now,String reason);
    VersionData appendApplied(long requirementId,long changeId,int nextVersion,ContentSnapshot content,long actorId,Instant now,String reason);
    PageData<VersionData> list(long requirementId,Paging paging);
    VersionData read(long requirementId,long versionId);
}

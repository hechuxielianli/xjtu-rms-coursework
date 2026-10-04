package com.example.rms.change.application.port;
import com.example.rms.change.application.*;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import java.time.Instant;
public interface ChangeStore {
    long requirementId(long changeId);
    long reviewChangeId(long reviewId);
    ChangeData read(long id);
    ChangeData lock(long id);
    boolean active(long requirementId);
    ChangeData create(long requirementId,long baseVersionId,String title,String reason,ContentSnapshot content,long actorId,Instant now);
    ChangeData edit(long id,String title,String reason,ContentSnapshot content,Instant now);
    ChangeData submit(long id,Instant now);
    ChangeData cancel(long id,Instant now);
    ChangeData afterDecision(long id,String decision,Instant now);
    ChangeData applied(long id,long actorId,Instant now);
    boolean pending(long id);
    int nextRound(long id);
    ChangeReviewData createReview(ChangeData change,int round,long actorId,Instant now);
    ChangeReviewData lockReview(long id);
    ChangeReviewData latestReview(long changeId);
    ChangeReviewData completeReview(long id,String decision,String comment,long actorId,Instant now);
    PageData<ChangeData> list(long requirementId,String status,Paging paging);
    PageData<ChangeReviewData> reviews(Long changeId,Paging paging);
}

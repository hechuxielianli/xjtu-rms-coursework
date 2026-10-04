package com.example.rms.review.application.port;
import com.example.rms.review.application.ReviewData;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import java.time.Instant;
public interface ReviewStore {
    long requirementId(long reviewId);
    ReviewData lock(long reviewId);
    boolean hasPending(long requirementId);
    int nextRound(long requirementId);
    ReviewData create(long requirementId,int round,ContentSnapshot content,long actorId,Instant now);
    ReviewData complete(long reviewId,String decision,String comment,long actorId,Instant now);
    PageData<ReviewData> list(Long requirementId,Paging paging);
}

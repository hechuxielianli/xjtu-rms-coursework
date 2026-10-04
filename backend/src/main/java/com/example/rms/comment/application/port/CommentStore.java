package com.example.rms.comment.application.port;
import com.example.rms.comment.application.CommentData;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.Paging;
import java.time.Instant;
public interface CommentStore {
    CommentData read(long id,boolean lock);
    PageData<CommentData> list(long requirementId,Paging paging);
    CommentData create(long requirementId,long actorId,String content,Instant now);
    CommentData delete(long id,long actorId,Instant now);
}

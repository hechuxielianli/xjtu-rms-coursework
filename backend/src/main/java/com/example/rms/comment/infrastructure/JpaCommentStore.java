package com.example.rms.comment.infrastructure;
import com.example.rms.comment.application.*;
import com.example.rms.comment.application.port.CommentStore;
import com.example.rms.comment.infrastructure.persistence.CommentEntity;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.stereotype.Repository;
@Repository
public class JpaCommentStore implements CommentStore,com.example.rms.shared.application.FailureTargetContext {
    private final EntityManager em;
    public JpaCommentStore(EntityManager em) { this.em=em; }
    @Override public boolean supports(String type) { return type.equals("COMMENT"); }
    @Override public Long requirementContext(String type,long id) { var rows=em.createQuery("select c.requirementId from CommentEntity c where c.commentId=:id",Long.class).setParameter("id",id).getResultList();return rows.isEmpty()?null:rows.getFirst(); }
    @Override public CommentData read(long id,boolean lock) { var c=lock?em.find(CommentEntity.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(CommentEntity.class,id);if(c==null)throw new RmsException(ErrorCode.NOT_FOUND);if(lock)em.refresh(c,LockModeType.PESSIMISTIC_WRITE);return data(c); }
    @Override public PageData<CommentData> list(long requirementId,Paging p) { var items=em.createQuery("select c from CommentEntity c where c.requirementId=:id order by c.commentId",CommentEntity.class).setParameter("id",requirementId).setFirstResult(p.offset()).setMaxResults(p.size()).getResultList().stream().map(JpaCommentStore::data).toList();long count=em.createQuery("select count(c) from CommentEntity c where c.requirementId=:id",Long.class).setParameter("id",requirementId).getSingleResult();return new PageData<>(items,p.page(),p.size(),count); }
    @Override public CommentData create(long requirementId,long actorId,String content,Instant now) { var c=CommentEntity.publish(requirementId,actorId,content,now);em.persist(c);em.flush();return data(c); }
    @Override public CommentData delete(long id,long actorId,Instant now) { var c=em.find(CommentEntity.class,id);c.deleteOwn(actorId,now);em.flush();return data(c); }
    private static CommentData data(CommentEntity c) { return new CommentData(c.getCommentId(),c.getRequirementId(),c.getAuthorId(),c.getContent(),c.getCreatedAt(),c.getIsDeleted(),c.getDeletedBy(),c.getDeletedAt()); }
}

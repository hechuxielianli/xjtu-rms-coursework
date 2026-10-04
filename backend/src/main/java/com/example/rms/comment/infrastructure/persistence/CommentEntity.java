package com.example.rms.comment.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="Comment")
public class CommentEntity {
    protected CommentEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="commentId", nullable=false, updatable=false)
    private Long commentId;
    @Column(name="requirementId", nullable=false, updatable=false)
    private Long requirementId;
    @Column(name="authorId", nullable=false, updatable=false)
    private Long authorId;
    @Column(name="content", nullable=false, columnDefinition="text", updatable=false)
    private String content;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="createdAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant createdAt;
    @Column(name="isDeleted", nullable=false)
    private Byte isDeleted;
    @Column(name="deletedBy", nullable=true)
    private Long deletedBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="deletedAt", nullable=true, columnDefinition="datetime(6)")
    private Instant deletedAt;
    public Long getCommentId() { return commentId; }
    public Long getRequirementId() { return requirementId; }
    public Long getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Byte getIsDeleted() { return isDeleted; }
    public Long getDeletedBy() { return deletedBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public static CommentEntity publish(long requirementId,long actorId,String content,Instant now) { CommentEntity c=new CommentEntity();c.requirementId=requirementId;c.authorId=actorId;c.content=content;c.createdAt=now;c.isDeleted=0;return c; }
    public void deleteOwn(long actorId,Instant now) { this.isDeleted=1;this.deletedBy=actorId;this.deletedAt=now; }
}

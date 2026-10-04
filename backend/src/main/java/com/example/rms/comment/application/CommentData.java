package com.example.rms.comment.application;
import java.time.Instant;
public record CommentData(long commentId,long requirementId,long authorId,String content,Instant createdAt,byte isDeleted,Long deletedBy,Instant deletedAt) {}

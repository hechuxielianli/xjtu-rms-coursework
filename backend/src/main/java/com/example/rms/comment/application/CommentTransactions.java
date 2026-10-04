package com.example.rms.comment.application;
import com.example.rms.comment.application.port.CommentStore;
import com.example.rms.requirement.application.contract.RequirementMutationService;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CommentTransactions {
    private final CommentStore store;private final RequirementMutationService requirements;private final AuditWriteService audit;private final Clock clock;
    public CommentTransactions(CommentStore store,RequirementMutationService requirements,AuditWriteService audit,Clock clock) { this.store=store;this.requirements=requirements;this.audit=audit;this.clock=clock; }
    @Transactional(rollbackFor=Exception.class) public CommentData create(Actor actor,long requirementId,String content) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER,RoleCode.REVIEWER,RoleCode.PROJECT_MEMBER);InputPolicy.nonBlank(content,Integer.MAX_VALUE);requirements.lockContext(requirementId);var after=store.create(requirementId,actor.userId(),content,now());audit.success(actor,new AuditChange(requirementId,"COMMENT",Long.toString(after.commentId()),"COMMENT_CREATE",null,safe(after)));return after; }
    @Transactional(rollbackFor=Exception.class) public void delete(Actor actor,long id) { actor.requireAny(RoleCode.REQUIREMENT_ENGINEER,RoleCode.REVIEWER,RoleCode.PROJECT_MEMBER);var located=store.read(id,false);requirements.lockContext(located.requirementId());var before=store.read(id,true);if(before.authorId()!=actor.userId())throw new RmsException(ErrorCode.NOT_AUTHOR);if(before.isDeleted()!=0)throw new RmsException(ErrorCode.STATE_CONFLICT);var after=store.delete(id,actor.userId(),now());audit.success(actor,new AuditChange(before.requirementId(),"COMMENT",Long.toString(id),"COMMENT_DELETE",safe(before),safe(after))); }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static Map<String,Object> safe(CommentData c) { Map<String,Object> values=new LinkedHashMap<>();values.put("commentId",c.commentId());values.put("requirementId",c.requirementId());values.put("content",c.isDeleted()==0?c.content():null);values.put("isDeleted",c.isDeleted());return values; }
}

package com.example.rms.comment.application;
import com.example.rms.comment.application.port.CommentStore;
import com.example.rms.requirement.application.contract.RequirementMutationService;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CommentService {
    private final CommentStore store;private final CommentTransactions writes;private final RequirementMutationService requirements;private final CurrentActor current;private final FailureAuditService failures;
    public CommentService(CommentStore store,CommentTransactions writes,RequirementMutationService requirements,CurrentActor current,FailureAuditService failures) { this.store=store;this.writes=writes;this.requirements=requirements;this.current=current;this.failures=failures; }
    @Transactional(readOnly=true) public PageData<CommentData> list(String requirementId,int page,int size) { current.require().requireAny(RoleCode.values());long id=InputPolicy.decimalId(requirementId);requirements.readContext(id);var data=store.list(id,new Paging(page,size));return new PageData<>(data.items().stream().map(CommentService::safe).toList(),data.page(),data.size(),data.totalElements()); }
    public CommentData create(String id,String content) { return safe(attempt(AuditChange.requirementCandidate(id),"NEW","COMMENT_CREATE",actor->writes.create(actor,InputPolicy.decimalId(id),content))); }
    public void delete(String id) { attempt(id,"COMMENT_DELETE",actor->{writes.delete(actor,InputPolicy.decimalId(id));return null;}); }
    private <T>T attempt(String target,String action,Function<Actor,T> call) { return attempt(null,target,action,call); }
    private <T>T attempt(Long parent,String target,String action,Function<Actor,T> call) {
        return failures.attempt(current.require(),parent,"COMMENT",target,action,call);
    }
    private static CommentData safe(CommentData c) { return new CommentData(c.commentId(),c.requirementId(),c.authorId(),c.isDeleted()==0?c.content():null,c.createdAt(),c.isDeleted(),c.deletedBy(),c.deletedAt()); }
}

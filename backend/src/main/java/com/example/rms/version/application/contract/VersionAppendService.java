package com.example.rms.version.application.contract;
import com.example.rms.version.application.port.VersionStore;
import com.example.rms.shared.domain.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Caller must hold the owning Requirement row lock in this physical transaction. */
@Service
public class VersionAppendService {
    private final VersionStore store;
    public VersionAppendService(VersionStore store) { this.store=store; }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public VersionData appendInitial(long requirementId,long approvedReviewId,ContentSnapshot content,long actorId,Instant now) {
        if(requirementId<=0 || approvedReviewId<=0 || actorId<=0 || now==null)throw new RmsException(ErrorCode.INVALID_INPUT);content.requireComplete();
        if(store.count(requirementId)!=0)throw new RmsException(ErrorCode.STATE_CONFLICT);
        return store.appendInitial(requirementId,approvedReviewId,content,actorId,now,"Initial approval");
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public VersionData appendApplied(long requirementId,long appliedChangeId,long currentVersionId,ContentSnapshot content,long actorId,Instant now,String reason) { if(requirementId<=0 || appliedChangeId<=0 || actorId<=0 || now==null)throw new RmsException(ErrorCode.INVALID_INPUT);content.requireComplete();InputPolicy.nonBlank(reason,Integer.MAX_VALUE);var current=store.read(requirementId,currentVersionId);if(current.versionNo()==Integer.MAX_VALUE || store.count(requirementId)!=current.versionNo())throw new RmsException(ErrorCode.STATE_CONFLICT);return store.appendApplied(requirementId,appliedChangeId,current.versionNo()+1,content,actorId,now,reason); }
}

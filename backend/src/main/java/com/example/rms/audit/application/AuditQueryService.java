package com.example.rms.audit.application;
import com.example.rms.audit.application.port.AuditQueryPort;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuditQueryService {
    private final AuditQueryPort port;private final CurrentActor current;
    public AuditQueryService(AuditQueryPort port,CurrentActor current) { this.port=port;this.current=current; }
    @Transactional(readOnly=true) public PageData<AuditData> history(String id,int page,int size,String from,String to,String action) {
        current.require().requireAny(RoleCode.values());long requirement=InputPolicy.decimalId(id);var paging=new Paging(page,size);var filter=AuditFilter.parse(from,to,action);
        if(!port.requirementExists(requirement))throw new RmsException(ErrorCode.NOT_FOUND);
        return port.query(requirement,paging,filter);
    }
    @Transactional(readOnly=true) public PageData<AuditData> system(int page,int size,String from,String to,String action) {
        current.require().requireAny(RoleCode.ADMIN);return port.query(null,new Paging(page,size),AuditFilter.parse(from,to,action));
    }
}

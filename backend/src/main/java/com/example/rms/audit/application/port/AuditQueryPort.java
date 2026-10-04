package com.example.rms.audit.application.port;
import com.example.rms.audit.application.*;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.Paging;
public interface AuditQueryPort {
    boolean requirementExists(long id);
    PageData<AuditData> query(Long requirementId,Paging paging,AuditFilter filter);
}

package com.example.rms.auth.infrastructure;
import com.example.rms.audit.application.contract.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.RmsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
/** Security-layer denial is outside any business transaction and never copies a request body. */
@Component
public class SecurityDenialAudit implements HttpWriteFailureObserver {
    private final CurrentActor current;private final FailureAuditService audit;
    public SecurityDenialAudit(CurrentActor current,FailureAuditService audit) { this.current=current;this.audit=audit; }
    public void record(HttpServletRequest request) {
        record(request.getMethod(),request.getRequestURI(),true);
    }
    @Override public void inputFailed(String method,String path) { record(method,path,false); }
    private void record(String method,String uri,boolean denied) {
        Actor actor;try { actor=current.require(); }catch(RmsException unavailable){return;}
        String[] path=uri.split("/");if(path.length<4)return;
        String type=switch(path[3]){case "users"->"USER";case "requirements"->path.length>5 && path[5].equals("comments")?"COMMENT":path.length>5 && path[5].equals("changes")?"CHANGE_REQUEST":"REQUIREMENT";case "requirement-reviews"->"REVIEW";case "changes"->"CHANGE_REQUEST";case "change-reviews"->"CHANGE_REVIEW";case "relations"->"RELATION";case "tags"->"TAG";case "comments"->"COMMENT";default->null;};
        if(type==null || method.equals("GET"))return;
        String target=path.length>4 && path[4].matches("[1-9][0-9]{0,18}")?path[4]:"NEW";
        if((type.equals("COMMENT") || type.equals("CHANGE_REQUEST")) && path[3].equals("requirements"))target="NEW";
        Long parent=path[3].equals("requirements") && path.length>4?AuditChange.requirementCandidate(path[4]):null;
        var change=new AuditChange(parent,type,target,denied?"HTTP_WRITE_DENIED":"HTTP_INPUT_FAILED",null,null);
        if(denied)audit.denied(actor,change);else audit.failed(actor,change);
    }
}

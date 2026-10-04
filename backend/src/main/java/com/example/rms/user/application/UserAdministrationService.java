package com.example.rms.user.application;
import com.example.rms.user.application.port.UserStore;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import com.example.rms.audit.application.contract.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Public write facade remains outside transactions; failures are audited after the inner proxy rolls back. */
@Service
public class UserAdministrationService {
    private final UserTransactions writes;private final UserStore store;private final CurrentActor current;private final FailureAuditService failures;
    public UserAdministrationService(UserTransactions writes,UserStore store,CurrentActor current,FailureAuditService failures) { this.writes=writes;this.store=store;this.current=current;this.failures=failures; }
    @Transactional(readOnly=true) public PageData<UserData> list(int page,int size) { current.require().requireAny(RoleCode.ADMIN);return store.list(new Paging(page,size)); }
    public UserData create(String username,String email,String displayName,String password,List<String> roles) { return attempt("NEW","USER_CREATE",actor->writes.create(actor,username,email,displayName,password,roles)); }
    public UserData edit(String id,Map<String,Object> fields) { return attempt(id,"USER_PROFILE_EDIT",actor->writes.edit(actor,InputPolicy.decimalId(id),fields)); }
    public UserData roles(String id,Map<String,Object> fields) { return attempt(id,"USER_ROLES_SET",actor->writes.roles(actor,InputPolicy.decimalId(id),fields)); }
    public UserData enabled(String id,String revision,boolean enabled) { return attempt(id,enabled?"USER_ENABLE":"USER_DISABLE",actor->writes.enabled(actor,InputPolicy.decimalId(id),revision,enabled)); }
    private UserData attempt(String target,String action,Function<Actor,UserData> call) {
        return failures.attempt(current.require(),null,"USER",target,action,call);
    }
}

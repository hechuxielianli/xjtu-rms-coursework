package com.example.rms.user.application;
import com.example.rms.user.application.port.UserStore;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import com.example.rms.audit.application.contract.*;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class UserTransactions {
    private static final class EmailField { @Email String value; }
    private final UserStore store;private final AuditWriteService audit;private final PasswordEncoder encoder;private final Validator validator;private final Clock clock;
    public UserTransactions(UserStore store,AuditWriteService audit,PasswordEncoder encoder,Validator validator,Clock clock) { this.store=store;this.audit=audit;this.encoder=encoder;this.validator=validator;this.clock=clock; }
    @Transactional(rollbackFor=Exception.class)
    public UserData create(Actor actor,String username,String email,String displayName,String initialPassword,List<String> rawRoles) {
        actor.requireAny(RoleCode.ADMIN);username=InputPolicy.nonBlank(username,64);email=email(email);displayName=InputPolicy.nonBlank(displayName,100);String password=PasswordInputPolicy.validate(initialPassword);Set<RoleCode> roles=roles(rawRoles);if(roles.isEmpty())throw new RmsException(ErrorCode.INVALID_INPUT);
        UserData user=store.create(username,email,displayName,encoder.encode(password),roles,actor.userId(),now());success(actor,"USER_CREATE",null,user);return user;
    }
    @Transactional(rollbackFor=Exception.class)
    public UserData edit(Actor actor,long id,Map<String,Object> values) {
        actor.requireAny(RoleCode.ADMIN);var patch=new InputPatch(values,Set.of("username","email","displayName","expectedLockVersion"),"expectedLockVersion");UserData before=store.lock(id);InputPolicy.expected(InputPolicy.lockVersion(patch.string("expectedLockVersion",false)),before.lockVersion());
        UserData after=store.edit(id,patch.has("username")?InputPolicy.nonBlank(patch.string("username",false),64):before.username(),patch.has("email")?email(patch.string("email",false)):before.email(),patch.has("displayName")?InputPolicy.nonBlank(patch.string("displayName",false),100):before.displayName(),now());success(actor,"USER_PROFILE_EDIT",before,after);return after;
    }
    @Transactional(rollbackFor=Exception.class)
    public UserData roles(Actor actor,long id,Map<String,Object> values) {
        actor.requireAny(RoleCode.ADMIN);var patch=new InputPatch(values,Set.of("roles","expectedLockVersion"),"expectedLockVersion");Set<RoleCode> requested=roles(patch.strings("roles"));UserData before=store.lock(id);InputPolicy.expected(InputPolicy.lockVersion(patch.string("expectedLockVersion",false)),before.lockVersion());
        if("ENABLED".equals(before.accountStatus()) && requested.isEmpty())throw new RmsException(ErrorCode.INVALID_INPUT);
        UserData after=store.roles(id,requested,actor.userId(),now());success(actor,"USER_ROLES_SET",before,after);return after;
    }
    @Transactional(rollbackFor=Exception.class)
    public UserData enabled(Actor actor,long id,String revision,boolean enabled) {
        actor.requireAny(RoleCode.ADMIN);long expected=InputPolicy.lockVersion(revision);UserData before=store.lock(id);InputPolicy.expected(expected,before.lockVersion());if(enabled && before.roles().isEmpty())throw new RmsException(ErrorCode.INVALID_INPUT);
        UserData after=store.enabled(id,enabled,now());success(actor,enabled?"USER_ENABLE":"USER_DISABLE",before,after);return after;
    }
    private String email(String value) { InputPolicy.nonBlank(value,254);if(!validator.validateValue(EmailField.class,"value",value).isEmpty())throw new RmsException(ErrorCode.INVALID_INPUT);return value; }
    private Set<RoleCode> roles(List<String> values) { if(values==null || new HashSet<>(values).size()!=values.size())throw new RmsException(ErrorCode.INVALID_INPUT);Set<RoleCode> roles=EnumSet.noneOf(RoleCode.class);try{for(String value:values)roles.add(RoleCode.valueOf(value));}catch(RuntimeException e){throw new RmsException(ErrorCode.INVALID_INPUT);}return roles; }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private Map<String,Object> safe(UserData user) { return Map.of("userId",user.userId(),"username",user.username(),"email",user.email(),"displayName",user.displayName(),"accountStatus",user.accountStatus(),"roles",user.roles().stream().map(Enum::name).toList(),"lockVersion",user.lockVersion()); }
    private void success(Actor actor,String action,UserData before,UserData after) { audit.success(actor,new AuditChange(null,"USER",Long.toString(after.userId()),action,before==null?null:safe(before),safe(after))); }
}

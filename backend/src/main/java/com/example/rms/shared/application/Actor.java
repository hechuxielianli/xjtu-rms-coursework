package com.example.rms.shared.application;
import com.example.rms.shared.domain.*;
import java.io.Serializable;
import java.util.Set;
/** Trusted, immutable server-side access projection; no passwords, emails or persistence entity. */
public record Actor(long userId,String displayName,String accountStatus,Set<RoleCode> roles) implements Serializable {
    public Actor { if(userId<=0)throw new IllegalArgumentException("Invalid actor");roles=Set.copyOf(roles); }
    public void requireAny(RoleCode... allowed) {
        if(!"ENABLED".equals(accountStatus)) throw new RmsException(ErrorCode.ACCOUNT_DISABLED);
        for(RoleCode role:allowed) if(roles.contains(role))return;
        throw new RmsException(ErrorCode.FORBIDDEN);
    }
}

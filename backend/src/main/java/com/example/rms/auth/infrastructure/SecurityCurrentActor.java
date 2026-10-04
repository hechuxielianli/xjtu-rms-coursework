package com.example.rms.auth.infrastructure;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import com.example.rms.user.application.contract.UserAccessService;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
@Component
public class SecurityCurrentActor implements CurrentActor {
    private final UserAccessService users;
    public SecurityCurrentActor(UserAccessService users) { this.users=users; }
    @Override public Optional<Actor> optional() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Actor saved))return Optional.empty();
        return Optional.of(users.requireEnabledActor(saved.userId()));
    }
    @Override public Actor require() { return optional().orElseThrow(()->new RmsException(ErrorCode.UNAUTHENTICATED)); }
}

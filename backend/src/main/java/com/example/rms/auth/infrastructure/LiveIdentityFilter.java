package com.example.rms.auth.infrastructure;
import com.example.rms.shared.api.ApiErrors;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.RmsException;
import com.example.rms.user.application.contract.UserAccessService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
/** Reload enabled state and role union for every protected request; no permanently cached permissions. */
public class LiveIdentityFilter extends OncePerRequestFilter {
    private final UserAccessService users;
    private final ApiErrors errors;
    public LiveIdentityFilter(UserAccessService users,ApiErrors errors) { this.users=users;this.errors=errors; }
    @Override protected boolean shouldNotFilter(HttpServletRequest r) { return !r.getRequestURI().startsWith("/api/v1/") || r.getRequestURI().equals("/api/v1/auth/login") || r.getRequestURI().equals("/api/v1/auth/csrf"); }
    @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && auth.getPrincipal() instanceof Actor saved) {
            try {
                Actor fresh=users.requireEnabledActor(saved.userId());
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(fresh,null,fresh.roles().stream().map(role->new SimpleGrantedAuthority("ROLE_"+role.name())).toList()));
            } catch(RmsException e) {
                SecurityContextHolder.clearContext();HttpSession session=r.getSession(false);if(session!=null)session.invalidate();errors.write(r,s,e.code());return;
            }
        }
        chain.doFilter(r,s);
    }
}

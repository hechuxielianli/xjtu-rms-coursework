package com.example.rms.auth.infrastructure;
import com.example.rms.shared.application.Actor;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.stereotype.Component;
/** Servlet-only boundary adapter; no business state or persistence accesses. */
@Component
public class ServletSessionBridge {
    private final HttpSessionSecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrf;
    public ServletSessionBridge(HttpSessionSecurityContextRepository contexts,HttpSessionCsrfTokenRepository csrf) { this.contexts=contexts;this.csrf=csrf; }
    public void establish(Actor actor,HttpServletRequest request,HttpServletResponse response) {
        var auth=UsernamePasswordAuthenticationToken.authenticated(actor,null,actor.roles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+r.name())).toList());
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth,request,response);
        new CsrfAuthenticationStrategy(csrf).onAuthentication(auth,request,response);
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);
    }
    public void invalidate(HttpServletRequest request,HttpServletResponse response) {
        var handler=new SecurityContextLogoutHandler();handler.setSecurityContextRepository(contexts);
        handler.logout(request,response,SecurityContextHolder.getContext().getAuthentication());
    }
}

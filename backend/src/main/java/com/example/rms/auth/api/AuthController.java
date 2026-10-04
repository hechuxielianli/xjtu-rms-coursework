package com.example.rms.auth.api;
import com.example.rms.auth.application.*;
import com.example.rms.auth.infrastructure.ServletSessionBridge;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.RoleCode;
import jakarta.servlet.http.*;
import java.util.Set;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    public record LoginRequest(String login,String password) { @Override public String toString() { return "LoginRequest[REDACTED]"; } }
    public record UserSummary(String userId,String displayName,String accountStatus) {}
    public record SessionResponse(UserSummary user,Set<RoleCode> roles) {}
    public record CsrfResponse(String headerName,String token) {}
    private final AuthenticationService authentication;
    private final SessionService sessions;
    private final ServletSessionBridge bridge;
    public AuthController(AuthenticationService authentication,SessionService sessions,ServletSessionBridge bridge) { this.authentication=authentication;this.sessions=sessions;this.bridge=bridge; }
    @PostMapping("/login") public ResponseEntity<SessionResponse> login(@RequestBody LoginRequest body,HttpServletRequest request,HttpServletResponse response) {
        Actor actor=authentication.login(new LoginCommand(body.login(),body.password()));bridge.establish(actor,request,response);return safe(session(actor));
    }
    @GetMapping("/session") public ResponseEntity<SessionResponse> session() { return safe(session(sessions.current())); }
    @GetMapping("/csrf") public ResponseEntity<CsrfResponse> csrf(CsrfToken token) { return safe(new CsrfResponse(token.getHeaderName(),token.getToken())); }
    @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response) { sessions.current();bridge.invalidate(request,response);return ResponseEntity.noContent().header("Cache-Control","no-store").build(); }
    private static SessionResponse session(Actor actor) { return new SessionResponse(new UserSummary(Long.toString(actor.userId()),actor.displayName(),actor.accountStatus()),actor.roles()); }
    private static <T> ResponseEntity<T> safe(T body) { return ResponseEntity.ok().header("Cache-Control","no-store").body(body); }
}

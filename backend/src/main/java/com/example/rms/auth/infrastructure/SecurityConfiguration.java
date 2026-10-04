package com.example.rms.auth.infrastructure;
import com.example.rms.shared.api.ApiErrors;
import com.example.rms.shared.domain.ErrorCode;
import com.example.rms.user.application.contract.UserAccessService;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean HttpSessionSecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean HttpSessionCsrfTokenRepository csrfRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http,ApiErrors errors,UserAccessService users,HttpSessionSecurityContextRepository contexts,HttpSessionCsrfTokenRepository csrf,SecurityDenialAudit denials)throws Exception {
        return http.securityContext(s->s.securityContextRepository(contexts).requireExplicitSave(true))
            .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/login","/api/v1/auth/csrf").permitAll()
                .requestMatchers("/api/v1/users","/api/v1/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/v1/audit-events").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/v1/requirements","/api/v1/tags").hasRole("REQUIREMENT_ENGINEER")
                .requestMatchers(HttpMethod.PATCH,"/api/v1/requirements/*/content","/api/v1/requirements/*/metadata","/api/v1/tags/*").hasRole("REQUIREMENT_ENGINEER")
                .requestMatchers(HttpMethod.POST,"/api/v1/requirements/*/submit","/api/v1/requirements/*/reopen").hasRole("REQUIREMENT_ENGINEER")
                .requestMatchers(HttpMethod.POST,"/api/v1/requirements/*/implement","/api/v1/requirements/*/verify").hasRole("PROJECT_MEMBER")
                .requestMatchers(HttpMethod.GET,"/api/v1/reviews/pending").hasRole("REVIEWER")
                .requestMatchers(HttpMethod.POST,"/api/v1/requirement-reviews/*/decision").hasRole("REVIEWER")
                .requestMatchers(HttpMethod.POST,"/api/v1/requirements/*/changes","/api/v1/changes/*/submit","/api/v1/changes/*/cancel","/api/v1/changes/*/apply").hasRole("REQUIREMENT_ENGINEER")
                .requestMatchers(HttpMethod.PATCH,"/api/v1/changes/*").hasRole("REQUIREMENT_ENGINEER")
                .requestMatchers(HttpMethod.GET,"/api/v1/change-reviews/pending").hasRole("REVIEWER")
                .requestMatchers(HttpMethod.POST,"/api/v1/change-reviews/*/decision").hasRole("REVIEWER")
                .requestMatchers(HttpMethod.POST,"/api/v1/relations","/api/v1/requirements/*/withdraw").hasRole("REQUIREMENT_ENGINEER")
                .requestMatchers(HttpMethod.DELETE,"/api/v1/relations/*").hasRole("REQUIREMENT_ENGINEER")
                .requestMatchers(HttpMethod.POST,"/api/v1/requirements/*/comments").hasAnyRole("REQUIREMENT_ENGINEER","REVIEWER","PROJECT_MEMBER")
                .requestMatchers(HttpMethod.DELETE,"/api/v1/comments/*").hasAnyRole("REQUIREMENT_ENGINEER","REVIEWER","PROJECT_MEMBER")
                .requestMatchers("/api/v1/**").authenticated().anyRequest().denyAll())
            .requestCache(c->c.disable()).formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,f)->errors.write(r,s,ErrorCode.UNAUTHENTICATED)).accessDeniedHandler((r,s,f)->{try{denials.record(r);}catch(RuntimeException auditFailure){errors.write(r,s,ErrorCode.INTERNAL_ERROR);return;}errors.write(r,s,ErrorCode.FORBIDDEN);}))
            .addFilterBefore(new LiveIdentityFilter(users,errors),AuthorizationFilter.class).build();
    }
}

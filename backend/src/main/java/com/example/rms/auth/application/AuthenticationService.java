package com.example.rms.auth.application;
import com.example.rms.user.application.contract.*;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class AuthenticationService {
    private final UserAccessService users;
    private final PasswordEncoder encoder;
    private final String absentUserHash;
    public AuthenticationService(UserAccessService users,PasswordEncoder encoder) { this.users=users;this.encoder=encoder;this.absentUserHash=encoder.encode(UUID.randomUUID().toString()); }
    public Actor login(LoginCommand command) {
        InputPolicy.nonBlank(command.login(),254);
        String password=PasswordInputPolicy.validate(command.password());
        var candidates=users.readCandidatesForAuthentication(command.login());
        if(candidates.size()>2)throw new IllegalStateException("AUTHENTICATION_CANDIDATE_CONTRACT_VIOLATION");

        AuthenticationIdentity selected=null;
        int matchingIdentities=0;
        // Check every candidate, padded to two comparisons; never prefer username/email or enabled status.
        for(int index=0;index<2;index++) {
            AuthenticationIdentity candidate=index<candidates.size()?candidates.get(index):null;
            boolean matched=encoder.matches(password,candidate==null?absentUserHash:candidate.passwordHash());
            if(candidate!=null && matched) {
                selected=candidate;
                matchingIdentities++;
            }
        }
        if(matchingIdentities!=1)throw new RmsException(ErrorCode.INVALID_CREDENTIALS);
        // Identity selection precedes the live enabled/role guard; disabled candidates are not filtered away.
        return users.requireEnabledActor(selected.actor().userId());
    }
}

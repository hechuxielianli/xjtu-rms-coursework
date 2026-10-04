package com.example.rms.auth.application;

import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import com.example.rms.user.application.contract.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** REG-AUTH-001: credential uniqueness, not alias order, selects the trusted identity. */
class AuthenticationCandidateRegressionTest {
    private final UserAccessService users=mock(UserAccessService.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private final Actor first=new Actor(1,"First","ENABLED",Set.of(RoleCode.VIEWER));
    private final Actor second=new Actor(2,"Second","ENABLED",Set.of(RoleCode.VIEWER));

    private AuthenticationService service(List<AuthenticationIdentity> candidates) {
        when(encoder.encode(any())).thenReturn("dummy-hash");
        when(users.readCandidatesForAuthentication("alias")).thenReturn(candidates);
        return new AuthenticationService(users,encoder);
    }
    private void twoComparisons() { verify(encoder,times(2)).matches(eq("submitted"),anyString()); }
    private RmsException invalid(AuthenticationService service) {
        RmsException failure=assertThrows(RmsException.class,()->service.login(new LoginCommand("alias","submitted")));
        assertEquals(ErrorCode.INVALID_CREDENTIALS,failure.code());
        twoComparisons();
        verify(users,never()).requireEnabledActor(anyLong());
        return failure;
    }

    @Test void uniqueCandidateUsesDummyPaddingAndLiveActor() {
        var service=service(List.of(new AuthenticationIdentity(first,"first-hash")));
        when(encoder.matches("submitted","first-hash")).thenReturn(true);
        when(users.requireEnabledActor(1)).thenReturn(first);
        assertEquals(first,service.login(new LoginCommand("alias","submitted")));
        twoComparisons();
        verify(encoder).matches("submitted","dummy-hash");
    }
    @Test void noCandidateStillUsesTwoComparisonsAndFails() { invalid(service(List.of())); }
    @Test void wrongPasswordForUniqueCandidateFailsWithSameCode() {
        invalid(service(List.of(new AuthenticationIdentity(first,"first-hash"))));
    }
    @Test void secondCandidateCanWinWithoutUsernameOrFirstRowPriority() {
        var service=service(List.of(new AuthenticationIdentity(first,"first-hash"),new AuthenticationIdentity(second,"second-hash")));
        when(encoder.matches("submitted","second-hash")).thenReturn(true);
        when(users.requireEnabledActor(2)).thenReturn(second);
        assertEquals(second,service.login(new LoginCommand("alias","submitted")));
        twoComparisons();
        verify(users,never()).requireEnabledActor(1);
    }
    @Test void firstMatchDoesNotShortCircuitOtherComparison() {
        var service=service(List.of(new AuthenticationIdentity(first,"first-hash"),new AuthenticationIdentity(second,"second-hash")));
        when(encoder.matches("submitted","first-hash")).thenReturn(true);
        when(users.requireEnabledActor(1)).thenReturn(first);
        assertEquals(first,service.login(new LoginCommand("alias","submitted")));
        twoComparisons();
        verify(encoder).matches("submitted","second-hash");
    }
    @Test void twoMatchingPasswordsAreIndistinguishableAndFailClosed() {
        var service=service(List.of(new AuthenticationIdentity(first,"first-hash"),new AuthenticationIdentity(second,"second-hash")));
        when(encoder.matches(eq("submitted"),anyString())).thenReturn(true);
        invalid(service);
    }
    @Test void twoCandidatesWithNoMatchingPasswordFailClosed() {
        invalid(service(List.of(new AuthenticationIdentity(first,"first-hash"),new AuthenticationIdentity(second,"second-hash"))));
    }
    @Test void uniquelyMatchingDisabledUserIsNotFilteredInFavorOfOtherCandidate() {
        Actor disabled=new Actor(1,"First","DISABLED",Set.of(RoleCode.VIEWER));
        var service=service(List.of(new AuthenticationIdentity(disabled,"first-hash"),new AuthenticationIdentity(second,"second-hash")));
        when(encoder.matches("submitted","first-hash")).thenReturn(true);
        when(users.requireEnabledActor(1)).thenThrow(new RmsException(ErrorCode.ACCOUNT_DISABLED));
        assertEquals(ErrorCode.ACCOUNT_DISABLED,assertThrows(RmsException.class,()->service.login(new LoginCommand("alias","submitted"))).code());
        twoComparisons();
        verify(users,never()).requireEnabledActor(2);
    }
    @Test void actualBcryptDifferentPasswordsResolveBothExistingCandidates() {
        PasswordEncoder bcrypt=spy(new BCryptPasswordEncoder());
        var candidates=List.of(new AuthenticationIdentity(first,bcrypt.encode("first synthetic value")),new AuthenticationIdentity(second,bcrypt.encode("second synthetic value")));
        when(users.readCandidatesForAuthentication("alias")).thenReturn(candidates);
        when(users.requireEnabledActor(1)).thenReturn(first);
        when(users.requireEnabledActor(2)).thenReturn(second);
        var service=new AuthenticationService(users,bcrypt);
        clearInvocations(bcrypt);
        assertEquals(first,service.login(new LoginCommand("alias","first synthetic value")));
        assertEquals(second,service.login(new LoginCommand("alias","second synthetic value")));
        verify(bcrypt,times(4)).matches(any(),anyString());
    }
    @Test void credentialProjectionAndCommandToStringRemainRedacted() {
        String projection=new AuthenticationIdentity(first,"sensitive-test-hash").toString();
        assertFalse(projection.contains("sensitive-test-hash"));
        assertFalse(new LoginCommand("alias","sensitive-test-password").toString().contains("sensitive-test-password"));
    }
}

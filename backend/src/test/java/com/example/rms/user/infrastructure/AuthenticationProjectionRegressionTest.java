package com.example.rms.user.infrastructure;

import com.example.rms.user.infrastructure.persistence.UserEntity;
import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Adapter contract check; actual collation/cross-alias behavior is also checked by the course HTTP runner. */
class AuthenticationProjectionRegressionTest {
    @Test void ownEqualAliasesReturnOneImmutableCandidateAndQueryIsBounded() {
        EntityManager em=mock(EntityManager.class);
        @SuppressWarnings("unchecked") TypedQuery<UserEntity> query=mock(TypedQuery.class);
        @SuppressWarnings("unchecked") TypedQuery<String> roles=mock(TypedQuery.class);
        when(em.createQuery(anyString(),eq(UserEntity.class))).thenReturn(query);
        when(query.setParameter("login","own-alias")).thenReturn(query);
        when(query.setMaxResults(2)).thenReturn(query);
        UserEntity user=mock(UserEntity.class);
        when(user.getUserId()).thenReturn(7L);
        when(user.getDisplayName()).thenReturn("Own alias");
        when(user.getAccountStatus()).thenReturn("ENABLED");
        when(user.getPasswordHash()).thenReturn("internal-test-hash");
        when(query.getResultList()).thenReturn(List.of(user));
        when(em.createQuery(anyString(),eq(String.class))).thenReturn(roles);
        when(roles.setParameter("id",7L)).thenReturn(roles);
        when(roles.getResultList()).thenReturn(List.of("VIEWER"));

        var candidates=new UserAccessAdapter(em).readCandidatesForAuthentication("own-alias");
        assertEquals(1,candidates.size());
        assertEquals(7,candidates.getFirst().actor().userId());
        assertThrows(UnsupportedOperationException.class,()->candidates.add(candidates.getFirst()));
        verify(query).setMaxResults(2);
        verify(query).setParameter("login","own-alias");
        assertFalse(candidates.toString().contains("internal-test-hash"));
    }
}

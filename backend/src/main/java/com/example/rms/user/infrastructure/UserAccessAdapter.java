package com.example.rms.user.infrastructure;
import com.example.rms.user.application.contract.*;
import com.example.rms.user.infrastructure.persistence.UserEntity;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @Transactional(readOnly=true)
public class UserAccessAdapter implements UserAccessService {
    private final EntityManager em;
    public UserAccessAdapter(EntityManager em) { this.em=em; }
    @Override public UserSummaryData readSummary(long userId) {
        UserEntity user=em.find(UserEntity.class,userId);if(user==null)throw new RmsException(ErrorCode.NOT_FOUND);
        return new UserSummaryData(user.getUserId(),user.getDisplayName(),user.getAccountStatus());
    }
    @Override public List<AuthenticationIdentity> readCandidatesForAuthentication(String login) {
        List<UserEntity> matches=em.createQuery("select u from UserEntity u where u.username=:login or u.email=:login",UserEntity.class).setParameter("login",login).setMaxResults(2).getResultList();
        // The two field unique keys bound this query; a user's own username=email is one identity.
        Map<Long,AuthenticationIdentity> candidates=new LinkedHashMap<>();
        for(UserEntity user:matches)candidates.putIfAbsent(user.getUserId(),new AuthenticationIdentity(actor(user),user.getPasswordHash()));
        return List.copyOf(candidates.values());
    }
    @Override public Actor requireEnabledActor(long userId) {
        UserEntity user=em.find(UserEntity.class,userId);
        if(user==null)throw new RmsException(ErrorCode.UNAUTHENTICATED);
        Actor actor=actor(user);
        if(!"ENABLED".equals(actor.accountStatus()))throw new RmsException(ErrorCode.ACCOUNT_DISABLED);
        if(actor.roles().isEmpty())throw new RmsException(ErrorCode.FORBIDDEN);
        return actor;
    }
    @Override public Actor requireEnabledAssignee(long userId) {
        UserEntity user=em.find(UserEntity.class,userId);
        if(user==null)throw new RmsException(ErrorCode.NOT_FOUND);
        Actor actor=actor(user);if(!"ENABLED".equals(actor.accountStatus()))throw new RmsException(ErrorCode.INVALID_INPUT);return actor;
    }
    private Actor actor(UserEntity user) {
        List<String> codes=em.createQuery("select r.roleCode from RoleEntity r,UserRoleEntity ur where ur.userId=:id and ur.roleId=r.roleId",String.class).setParameter("id",user.getUserId()).getResultList();
        Set<RoleCode> roles=new HashSet<>();for(String code:codes)roles.add(RoleCode.valueOf(code));
        return new Actor(user.getUserId(),user.getDisplayName(),user.getAccountStatus(),roles);
    }
}

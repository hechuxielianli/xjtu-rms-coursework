package com.example.rms.user.infrastructure;
import com.example.rms.user.application.*;
import com.example.rms.user.application.port.UserStore;
import com.example.rms.user.infrastructure.persistence.*;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class JpaUserStore implements UserStore {
    private final EntityManager em;
    public JpaUserStore(EntityManager em) { this.em=em; }
    @Override public PageData<UserData> list(Paging p) { var items=em.createQuery("select u from UserEntity u order by u.userId",UserEntity.class).setFirstResult(p.offset()).setMaxResults(p.size()).getResultList();return new PageData<>(items.stream().map(this::data).toList(),p.page(),p.size(),em.createQuery("select count(u) from UserEntity u",Long.class).getSingleResult()); }
    @Override public UserData lock(long id) { UserEntity user=em.find(UserEntity.class,id,LockModeType.PESSIMISTIC_WRITE);if(user==null)throw new RmsException(ErrorCode.NOT_FOUND);return data(user); }
    @Override public UserData create(String username,String email,String displayName,String hash,Set<RoleCode> roles,long actorId,Instant now) { UserEntity user=UserEntity.bootstrap(username,email,displayName,hash,now);em.persist(user);em.flush();changeRoles(user,roles,actorId,now);em.flush();return data(user); }
    @Override public UserData edit(long id,String username,String email,String displayName,Instant now) { UserEntity user=em.find(UserEntity.class,id);user.editProfile(username,email,displayName,now);em.flush();return data(user); }
    @Override public UserData roles(long id,Set<RoleCode> roles,long actorId,Instant now) { UserEntity user=em.find(UserEntity.class,id);changeRoles(user,roles,actorId,now);user.touch(now);em.flush();return data(user); }
    @Override public UserData enabled(long id,boolean enabled,Instant now) { UserEntity user=em.find(UserEntity.class,id);user.changeEnabled(enabled,now);em.flush();return data(user); }
    private void changeRoles(UserEntity user,Set<RoleCode> requested,long actorId,Instant now) {
        Map<Long,RoleCode> catalog=catalog();Set<RoleCode> retained=new HashSet<>();
        for(UserRoleEntity link:links(user.getUserId())) { RoleCode role=catalog.get(link.getRoleId());if(!requested.contains(role))em.remove(link);else retained.add(role); }
        for(var role:catalog.entrySet())if(requested.contains(role.getValue()) && !retained.contains(role.getValue()))em.persist(UserRoleEntity.grant(user.getUserId(),role.getKey(),actorId,now));
    }
    private List<UserRoleEntity> links(long id) { return em.createQuery("select ur from UserRoleEntity ur where ur.userId=:id order by ur.roleId",UserRoleEntity.class).setParameter("id",id).getResultList(); }
    private Map<Long,RoleCode> catalog() { Map<Long,RoleCode> roles=new LinkedHashMap<>();for(RoleEntity role:em.createQuery("select r from RoleEntity r order by r.roleId",RoleEntity.class).getResultList())roles.put(role.getRoleId(),RoleCode.valueOf(role.getRoleCode()));return roles; }
    private UserData data(UserEntity u) { Map<Long,RoleCode> catalog=catalog();var links=links(u.getUserId());return new UserData(u.getUserId(),u.getUsername(),u.getEmail(),u.getDisplayName(),u.getAccountStatus(),u.getCreatedAt(),u.getUpdatedAt(),u.getLockVersion(),links.stream().map(link->catalog.get(link.getRoleId())).sorted().toList(),links.stream().map(link->new UserData.Assignment(link.getUserId(),link.getRoleId(),link.getGrantedBy(),link.getGrantedAt())).toList()); }
}

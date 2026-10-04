package com.example.rms.user.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="UserRole")
@IdClass(UserRoleId.class)
@org.hibernate.annotations.Immutable
public class UserRoleEntity {
    protected UserRoleEntity() {}
    @Id
    @Column(name="userId", nullable=false, updatable=false)
    private Long userId;
    @Id
    @Column(name="roleId", nullable=false, updatable=false)
    private Long roleId;
    @Column(name="grantedBy", nullable=false, updatable=false)
    private Long grantedBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="grantedAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant grantedAt;
    public Long getUserId() { return userId; }
    public Long getRoleId() { return roleId; }
    public Long getGrantedBy() { return grantedBy; }
    public Instant getGrantedAt() { return grantedAt; }
    public static UserRoleEntity bootstrap(Long userId,Long roleId,Instant now) { UserRoleEntity r=new UserRoleEntity();r.userId=userId;r.roleId=roleId;r.grantedBy=userId;r.grantedAt=now;return r; }
    public static UserRoleEntity grant(long userId,long roleId,long grantedBy,Instant now) { UserRoleEntity r=new UserRoleEntity();r.userId=userId;r.roleId=roleId;r.grantedBy=grantedBy;r.grantedAt=now;return r; }
}

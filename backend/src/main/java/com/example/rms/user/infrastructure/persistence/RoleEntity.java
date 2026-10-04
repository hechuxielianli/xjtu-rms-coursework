package com.example.rms.user.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="Role")
@org.hibernate.annotations.Immutable
public class RoleEntity {
    protected RoleEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="roleId", nullable=false, updatable=false)
    private Long roleId;
    @Column(name="roleCode", nullable=false, length=32, updatable=false)
    private String roleCode;
    @Column(name="roleName", nullable=false, length=100, updatable=false)
    private String roleName;
    @Column(name="description", nullable=false, length=500, updatable=false)
    private String description;
    public Long getRoleId() { return roleId; }
    public String getRoleCode() { return roleCode; }
    public String getRoleName() { return roleName; }
    public String getDescription() { return description; }
    public static RoleEntity predefined(String code,String name,String description) { RoleEntity r=new RoleEntity();r.roleCode=code;r.roleName=name;r.description=description;return r; }
}

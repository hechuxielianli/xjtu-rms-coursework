package com.example.rms.user.infrastructure.persistence;
import java.io.Serializable;
import java.util.Objects;
public class UserRoleId implements Serializable {
    private static final long serialVersionUID=1L;
    public Long userId;
    public Long roleId;
    public UserRoleId() {}
    public UserRoleId(Long userId,Long roleId) {this.userId=userId; this.roleId=roleId;}
    @Override public boolean equals(Object o) { return o instanceof UserRoleId other && Objects.equals(userId,other.userId) && Objects.equals(roleId,other.roleId); }
    @Override public int hashCode() { return Objects.hash(userId,roleId); }
}

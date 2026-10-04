package com.example.rms.user.application;
import com.example.rms.shared.domain.RoleCode;
import java.time.Instant;
import java.util.List;
public record UserData(long userId,String username,String email,String displayName,String accountStatus,Instant createdAt,Instant updatedAt,long lockVersion,List<RoleCode> roles,List<Assignment> assignments) {
    public record Assignment(long userId,long roleId,long grantedBy,Instant grantedAt) {}
}

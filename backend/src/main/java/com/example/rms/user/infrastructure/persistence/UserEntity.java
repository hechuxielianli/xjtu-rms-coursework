package com.example.rms.user.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen Baseline 1.3 persistence projection; IDs cross modules as values, never entities. */
@Entity
@Table(name="User")
public class UserEntity {
    protected UserEntity() {}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="userId", nullable=false, updatable=false)
    private Long userId;
    @Column(name="username", nullable=false, length=64)
    private String username;
    @Column(name="email", nullable=false, length=254)
    private String email;
    @Column(name="passwordHash", nullable=false, length=255)
    private String passwordHash;
    @Column(name="displayName", nullable=false, length=100)
    private String displayName;
    @Column(name="accountStatus", nullable=false, length=32)
    private String accountStatus;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="createdAt", nullable=false, columnDefinition="datetime(6)", updatable=false)
    private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="updatedAt", nullable=false, columnDefinition="datetime(6)")
    private Instant updatedAt;
    @Version
    @Column(name="lockVersion", nullable=false)
    private Long lockVersion;
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getAccountStatus() { return accountStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getLockVersion() { return lockVersion; }
    public static UserEntity bootstrap(String username,String email,String displayName,String hash,Instant now) {
        UserEntity u=new UserEntity();u.username=username;u.email=email;u.displayName=displayName;u.passwordHash=hash;u.accountStatus="ENABLED";u.createdAt=now;u.updatedAt=now;u.lockVersion=0L;return u;
    }
    public void editProfile(String username,String email,String displayName,Instant now) { this.username=username;this.email=email;this.displayName=displayName;touch(now); }
    public void changeEnabled(boolean enabled,Instant now) { this.accountStatus=enabled?"ENABLED":"DISABLED";touch(now); }
    public void touch(Instant now) { this.updatedAt=now.isAfter(updatedAt)?now:updatedAt.plus(1,java.time.temporal.ChronoUnit.MICROS); }
}

package com.example.rms.user.infrastructure;
import com.example.rms.user.infrastructure.persistence.*;
import com.example.rms.shared.domain.RoleCode;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.sql.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
/** Trusted deployment-only initializer. Separate lock lease surrounds complete JPA seed transaction. */
@Component
public class TrustedBootstrapInitializer implements ApplicationRunner {
    private static final String LOCK="rms_bootstrap_initialization";
    private static final List<String> BUSINESS_TABLES=List.of("User","UserRole","Requirement","RequirementVersion","RequirementReview","ChangeRequest","ChangeRequestReview","RequirementRelation","Tag","RequirementTag","Comment","AuditEvent");
    private final DataSource datasource;
    private final EntityManager em;
    private final TransactionTemplate transaction;
    private final Environment environment;
    private final Validator validator;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public TrustedBootstrapInitializer(DataSource ds,EntityManager em,PlatformTransactionManager manager,Environment env,Validator validator,PasswordEncoder encoder,Clock clock) {
        this.datasource=ds;this.em=em;this.transaction=new TransactionTemplate(manager);this.environment=env;this.validator=validator;this.encoder=encoder;this.clock=clock;
    }
    @Override public void run(ApplicationArguments arguments) { initialize(); }
    public void initialize() {
        boolean acquired=false;
        try(Connection lease=datasource.getConnection()) {
            try {
                Number lock=scalar(lease,"SELECT GET_LOCK(?,10)",LOCK);
                if(lock==null || lock.intValue()!=1)throw new IllegalStateException("BOOTSTRAP_LOCK_UNAVAILABLE");
                acquired=true;
                transaction.executeWithoutResult(status -> initializeInTransaction());
            } finally {
                if(acquired) {
                    try { Number release=scalar(lease,"SELECT RELEASE_LOCK(?)",LOCK);if(release==null || release.intValue()!=1)throw new SQLException("Release failed"); }
                    catch(SQLException releaseFailure) {
                        // Never normal-return a deployment lease that might still own a session lock.
                        HikariDataSource pool=datasource.unwrap(HikariDataSource.class);pool.evictConnection(lease);lease.abort(Runnable::run);
                        throw new IllegalStateException("BOOTSTRAP_LOCK_CLEANUP_FAILED");
                    }
                }
            }
        } catch(IllegalStateException e) {
            if(e.getMessage()!=null && e.getMessage().startsWith("BOOTSTRAP_"))throw e;
            throw new IllegalStateException("BOOTSTRAP_INITIALIZATION_FAILED");
        } catch(Exception e) { throw new IllegalStateException("BOOTSTRAP_INITIALIZATION_FAILED"); }
    }
    private void initializeInTransaction() {
        boolean empty=true;
        for(String table:BUSINESS_TABLES)if(((Number)em.createNativeQuery("SELECT COUNT(*) FROM `"+table+"`").getSingleResult()).longValue()!=0){empty=false;break;}
        if(!empty) {
            Number admins=(Number)em.createNativeQuery("SELECT COUNT(DISTINCT u.userId) FROM `User` u JOIN `UserRole` ur ON ur.userId=u.userId JOIN `Role` r ON r.roleId=ur.roleId WHERE u.accountStatus='ENABLED' AND r.roleCode='ADMIN'").getSingleResult();
            if(admins.longValue()==0)throw new IllegalStateException("BOOTSTRAP_ADMIN_UNAVAILABLE");
            Number catalog=(Number)em.createNativeQuery("SELECT COUNT(*) FROM `Role`").getSingleResult();
            if(catalog.intValue()!=5)throw new IllegalStateException("BOOTSTRAP_CATALOG_INVALID");
            return;
        }
        for(RoleCode code:RoleCode.values()) {
            List<RoleEntity> role=em.createQuery("select r from RoleEntity r where r.roleCode=:code",RoleEntity.class).setParameter("code",code.name()).getResultList();
            if(role.isEmpty())em.persist(RoleEntity.predefined(code.name(),code.name(),"Predefined RMS Baseline 1.3 role"));
        }
        BootstrapConfiguration config=BootstrapConfiguration.validated(environment,validator);
        var now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        UserEntity user=UserEntity.bootstrap(config.username(),config.email(),config.displayName(),encoder.encode(config.password()),now);
        em.persist(user);em.flush();
        RoleEntity admin=em.createQuery("select r from RoleEntity r where r.roleCode='ADMIN'",RoleEntity.class).getSingleResult();
        em.persist(UserRoleEntity.bootstrap(user.getUserId(),admin.getRoleId(),now));em.flush();
    }
    private static Number scalar(Connection connection,String sql,String value)throws SQLException {
        try(PreparedStatement s=connection.prepareStatement(sql)){s.setString(1,value);try(ResultSet r=s.executeQuery()){r.next();return (Number)r.getObject(1);}}
    }
}

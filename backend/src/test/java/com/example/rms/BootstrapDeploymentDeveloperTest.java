package com.example.rms;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;
import static org.junit.jupiter.api.Assertions.*;
/** Real isolated MySQL bootstrap deployment trials; only exact owned probe schema is reset. */
class BootstrapDeploymentDeveloperTest {
    private final String url=System.getenv("RMS_BOOTSTRAP_PROBE_DB_URL");
    private final String username=System.getenv("RMS_DB_USERNAME"),password=System.getenv("RMS_DB_PASSWORD");
    private Connection connection()throws SQLException { return DriverManager.getConnection(url,username,password); }
    private long count(String table)throws SQLException { try(var c=connection();var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM `"+table+"`")){r.next();return r.getLong(1);} }
    private void start(String rawPassword) {
        Map<String,Object> values=new HashMap<>();values.put("spring.datasource.url",url);values.put("spring.datasource.username",username);values.put("spring.datasource.password",password);
        values.put("server.address","127.0.0.1");values.put("server.port","0");values.put("RMS_BOOTSTRAP_USERNAME","wave0_probe_admin");values.put("RMS_BOOTSTRAP_EMAIL","wave0-probe@example.invalid");values.put("RMS_BOOTSTRAP_DISPLAY_NAME","Wave0 synthetic deployment probe");values.put("RMS_BOOTSTRAP_PASSWORD",rawPassword);
        values.put("logging.level.root","WARN");values.put("spring.main.banner-mode","off");
        var env=new StandardServletEnvironment();env.getPropertySources().addFirst(new MapPropertySource("owned-wave0-probe",values));
        var application=new SpringApplication(RmsApplication.class);application.setEnvironment(env);
        try(var context=application.run()) { assertTrue(context.isActive()); }
    }
    @Test void realEmptyFailureRollbackSuccessIdempotenceAndNoAdminFailFast()throws Exception {
        assertNotNull(url,"Explicit owned bootstrap probe database required");
        try(var c=connection();var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT DATABASE()")){r.next();assertEquals("rms_v1_bootstrap_probe",r.getString(1),"Refuse to reset another schema");}
            s.execute("SET FOREIGN_KEY_CHECKS=0");try{for(String table:List.of("AuditEvent","Comment","RequirementTag","Tag","RequirementRelation","ChangeRequestReview","ChangeRequest","RequirementReview","RequirementVersion","Requirement","UserRole","Role","User","flyway_schema_history"))s.execute("DROP TABLE IF EXISTS `"+table+"`");}finally{s.execute("SET FOREIGN_KEY_CHECKS=1");}
        }
        RuntimeException invalid=assertThrows(RuntimeException.class,()->start("A".repeat(73)));assertTrue(safeCause(invalid).contains("BOOTSTRAP_CONFIG_INVALID"));assertEquals(0,count("User"));assertEquals(0,count("Role"));assertEquals(0,count("UserRole"));
        String synthetic="probe "+UUID.randomUUID()+" ";start(synthetic);assertEquals(1,count("User"));assertEquals(5,count("Role"));assertEquals(1,count("UserRole"));
        try(var c=connection();var s=c.createStatement();var r=s.executeQuery("SELECT ur.userId,ur.grantedBy,u.passwordHash FROM `UserRole` ur JOIN `User` u ON u.userId=ur.userId")){r.next();assertEquals(r.getLong(1),r.getLong(2));assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(synthetic,r.getString(3)));}
        start("");assertEquals(1,count("User"));assertEquals(1,count("UserRole"));
        try(var c=connection();var s=c.createStatement()){s.executeUpdate("UPDATE `User` SET accountStatus='DISABLED'");}
        RuntimeException missing=assertThrows(RuntimeException.class,()->start(synthetic));assertTrue(safeCause(missing).contains("BOOTSTRAP_ADMIN_UNAVAILABLE"));assertEquals(1,count("User"));assertEquals(1,count("UserRole"));
        try(var c=connection();var s=c.createStatement()){s.executeUpdate("UPDATE `User` SET accountStatus='ENABLED'");}
        System.out.println("WAVE0_BOOTSTRAP_REAL_PROBE: invalid config rollback; 5 roles; self ADMIN; BCrypt; idempotent without initial password; nonempty no-admin fail-fast; PASS");
    }
    private static String safeCause(Throwable exception) { while(exception.getCause()!=null)exception=exception.getCause();return exception.getMessage()==null?"":exception.getMessage(); }
}

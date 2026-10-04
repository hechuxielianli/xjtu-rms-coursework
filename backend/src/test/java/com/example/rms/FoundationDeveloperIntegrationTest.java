package com.example.rms;
import com.example.rms.audit.application.contract.*;
import com.example.rms.auth.infrastructure.*;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import com.example.rms.user.infrastructure.TrustedBootstrapInitializer;
import com.example.rms.user.application.contract.UserAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Developer-owned Wave0 probes only; never reads or modifies protected testing oracle. */
@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class FoundationDeveloperIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager manager;
    @Autowired TrustedBootstrapInitializer bootstrap;
    @Autowired UserAccessService users;
    @Autowired CurrentActor current;
    @Autowired AuditWriteService audit;
    @Autowired FailureAuditService failures;
    private long adminId() { return ((Number)em.createNativeQuery("SELECT MIN(u.userId) FROM `User` u JOIN `UserRole` ur ON ur.userId=u.userId JOIN `Role` r ON r.roleId=ur.roleId WHERE u.accountStatus='ENABLED' AND r.roleCode='ADMIN'").getSingleResult()).longValue(); }
    private Number count(String table) { return (Number)em.createNativeQuery("SELECT COUNT(*) FROM `"+table+"`").getSingleResult(); }
    private Actor trusted() { Actor actor=users.requireEnabledActor(adminId());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(actor,null,List.of()));return actor; }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }
    @Test void frozenSchemaAndAllMappedFields()throws Exception {
        var model=json.readTree(Files.readString(Path.of("../docs/baseline/rms-baseline-1.3.json")));
        String frozen=Files.readString(Path.of("../docs/contracts/database-schema.sql"));
        String migration=Files.readString(Path.of("src/main/resources/db/migration/V1__rms_baseline_1_3.sql"));
        assertEquals(frozen.substring(frozen.indexOf("SET NAMES utf8mb4;")),migration.substring(migration.indexOf("SET NAMES utf8mb4;")),"Frozen SQL body must be preserved");
        assertEquals(13,emf.getMetamodel().getEntities().size());int columns=0;
        for(var entity:model.get("entities")) {
            String table=entity.get("name").asText();Class<?> type=emf.getMetamodel().getEntities().stream().filter(t->t.getJavaType().getAnnotation(Table.class).name().equals(table)).findFirst().orElseThrow().getJavaType();
            Set<String> actual=new HashSet<>();for(var f:type.getDeclaredFields())if(f.isAnnotationPresent(Column.class))actual.add(f.getAnnotation(Column.class).name());
            Set<String> expected=new HashSet<>();for(var f:entity.get("fields"))expected.add(f.get("name").asText());assertEquals(expected,actual);columns+=actual.size();
            Map<String,Object[]> installed=new HashMap<>();
            for(Object raw:em.createNativeQuery("SELECT column_name,column_type,is_nullable,extra,generation_expression FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=:table").setParameter("table",table).getResultList()) { Object[] row=(Object[])raw;installed.put((String)row[0],row); }
            assertEquals(expected,installed.keySet(),"Installed fields: "+table);
            Map<String,Set<String>> keys=new TreeMap<>();
            for(var field:entity.get("fields")) {
                String name=field.get("name").asText();Object[] row=installed.get(name);var mapped=type.getDeclaredField(name);Column column=mapped.getAnnotation(Column.class);
                assertEquals(field.get("sql").asText().toLowerCase(Locale.ROOT),row[1],"SQL type: "+table+"."+name);
                assertEquals(field.get("nullable").asBoolean(),"YES".equals(row[2]),"SQL nullability: "+table+"."+name);
                assertEquals(field.get("nullable").asBoolean(),column.nullable(),"Mapping nullability: "+table+"."+name);
                if(model.get("generatedColumns").has(table+"."+name)) { assertFalse(column.insertable());assertFalse(column.updatable());assertTrue(((String)row[3]).contains("GENERATED"));assertFalse(((String)row[4]).isBlank()); }
                assertEquals(name.equals("lockVersion"),mapped.isAnnotationPresent(Version.class),"Only frozen lockVersion uses optimistic versioning");
                for(String key:field.get("key").asText().split("/")) {
                    if(key.startsWith("PK"))keys.computeIfAbsent("PRIMARY",k->new HashSet<>()).add(name);
                    else if(key.matches("U[0-9]+"))keys.computeIfAbsent("uk_"+table+"_"+key.substring(1),k->new HashSet<>()).add(name);
                }
            }
            Map<String,Set<String>> actualKeys=new TreeMap<>();
            for(Object raw:em.createNativeQuery("SELECT index_name,column_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=:table AND non_unique=0 ORDER BY index_name,seq_in_index").setParameter("table",table).getResultList()) { Object[] row=(Object[])raw;actualKeys.computeIfAbsent((String)row[0],k->new HashSet<>()).add((String)row[1]); }
            assertEquals(keys,actualKeys,"PK and candidate-key groups: "+table);
        }
        assertEquals(151,columns);
        assertEquals(13,((Number)em.createNativeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history'").getSingleResult()).intValue());
        assertEquals(34,((Number)em.createNativeQuery("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='FOREIGN KEY'").getSingleResult()).intValue());
        int tableCase=((Number)em.createNativeQuery("SELECT @@lower_case_table_names").getSingleResult()).intValue();
        for(var relationship:model.get("relationships")) {
            List<?> rows=em.createNativeQuery("SELECT k.table_name,k.column_name,k.referenced_table_name,k.referenced_column_name,r.delete_rule,r.update_rule FROM information_schema.key_column_usage k JOIN information_schema.referential_constraints r ON r.constraint_schema=k.constraint_schema AND r.constraint_name=k.constraint_name WHERE k.constraint_schema=DATABASE() AND k.constraint_name=:name ORDER BY k.ordinal_position").setParameter("name","fk_"+relationship.get("id").asText()).getResultList();
            assertEquals(relationship.get("local").size(),rows.size());
            for(int i=0;i<rows.size();i++) { Object[] row=(Object[])rows.get(i);assertEquals(metadataTable(relationship.get("child").asText(),tableCase),row[0]);assertEquals(relationship.get("local").get(i).asText(),row[1]);assertEquals(metadataTable(relationship.get("parent").asText(),tableCase),row[2]);assertEquals(relationship.get("remote").get(i).asText(),row[3]);assertEquals("RESTRICT",row[4]);assertEquals("RESTRICT",row[5]); }
        }
    }
    private static String metadataTable(String frozenName,int lowerCaseTableNames) { return lowerCaseTableNames==0?frozenName:frozenName.toLowerCase(Locale.ROOT); }
    @Test void bootstrapIsIdempotentAndSelfGrantedAdmin() {
        long before=count("User").longValue(),assignments=count("UserRole").longValue();bootstrap.initialize();bootstrap.initialize();assertEquals(before,count("User").longValue());assertEquals(assignments,count("UserRole").longValue());assertEquals(5,count("Role").intValue());
        assertEquals(1,((Number)em.createNativeQuery("SELECT COUNT(*) FROM `UserRole` ur JOIN `Role` r ON r.roleId=ur.roleId WHERE ur.userId=:id AND ur.grantedBy=ur.userId AND r.roleCode='ADMIN'").setParameter("id",adminId()).getSingleResult()).intValue());
    }
    @Test void cookieSessionEncodedCsrfStrictJsonAndLogout()throws Exception {
        mvc.perform(get("/api/v1/auth/session")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        MvcResult csrf=mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        MockHttpSession session=(MockHttpSession)csrf.getRequest().getSession(false);String original=session.getId(),token=json.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        String login=System.getenv("RMS_BOOTSTRAP_USERNAME"),password=System.getenv("RMS_BOOTSTRAP_PASSWORD");assertNotNull(login);assertNotNull(password);
        String body=json.writeValueAsString(Map.of("login",login,"password",password));
        mvc.perform(post("/api/v1/auth/login").session(session).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/login").session(session).header("X-CSRF-TOKEN",token).contentType("application/json").content(json.writeValueAsString(Map.of("login",login,"password",password,"actorId","1")))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        MvcResult signed=mvc.perform(post("/api/v1/auth/login").session(session).header("X-CSRF-TOKEN",token).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.user.userId").isString()).andExpect(jsonPath("$.user.accountStatus").value("ENABLED")).andExpect(jsonPath("$.roles[0]").value("ADMIN")).andReturn();
        assertNotEquals(original,session.getId());String response=signed.getResponse().getContentAsString();assertFalse(response.contains("password"));assertFalse(response.contains(password));assertFalse(response.contains("email"));
        mvc.perform(get("/api/v1/auth/session").session(session)).andExpect(status().isOk());
        String nextToken=json.readTree(mvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn().getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/v1/auth/logout").session(session).header("X-CSRF-TOKEN",nextToken)).andExpect(status().isNoContent());assertTrue(session.isInvalid());
        mvc.perform(get("/api/v1/auth/session")).andExpect(status().isUnauthorized());
    }
    @Test void currentActorUsesActualStateAndAdminHasNoImpliedRole() {
        Actor actor=trusted();assertThrows(RmsException.class,()->actor.requireAny(RoleCode.REQUIREMENT_ENGINEER));
        new TransactionTemplate(manager).executeWithoutResult(status->{
            em.createNativeQuery("UPDATE `User` SET accountStatus='DISABLED' WHERE userId=:id").setParameter("id",actor.userId()).executeUpdate();em.clear();
            assertEquals(ErrorCode.ACCOUNT_DISABLED,assertThrows(RmsException.class,()->current.require()).code());status.setRollbackOnly();
        });
        assertEquals("ENABLED",users.requireEnabledActor(actor.userId()).accountStatus());
    }
    @Test void auditSuccessSharesTransactionAndFailureOccursAfterRollback() {
        Actor actor=trusted();AuditChange change=new AuditChange(null,"USER",Long.toString(actor.userId()),"WAVE0_DEVELOPER_PROBE",null,Map.of("accountStatus","ENABLED"));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->audit.success(change));
        long before=count("AuditEvent").longValue();
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(status->{audit.success(change);em.flush();throw new IllegalStateException("DEVELOPER_ROLLBACK_PROBE");}));
        assertEquals(before,count("AuditEvent").longValue());
        failures.failed(actor,change);assertEquals(before+1,count("AuditEvent").longValue());
        assertThrows(IllegalArgumentException.class,()->new AuditChange(null,"USER","1","PROBE",null,Map.of("passwordHash","synthetic-forbidden")));
        new TransactionTemplate(manager).executeWithoutResult(status->{assertThrows(IllegalStateException.class,()->failures.denied(actor,change));status.setRollbackOnly();});
    }
}

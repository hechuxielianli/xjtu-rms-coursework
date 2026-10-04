package com.example.rms;
import com.example.rms.user.application.*;
import com.example.rms.user.application.contract.UserAccessService;
import com.example.rms.requirement.application.*;
import com.example.rms.requirement.application.contract.RequirementMutationService;
import com.example.rms.review.application.*;
import com.example.rms.change.application.*;
import com.example.rms.version.application.contract.VersionAppendService;
import com.example.rms.shared.application.Actor;
import com.example.rms.shared.domain.*;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;
import com.example.rms.audit.application.*;
import com.example.rms.audit.application.contract.*;
import com.example.rms.audit.infrastructure.persistence.AuditEventEntity;
import com.example.rms.requirement.infrastructure.persistence.RequirementEntity;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/** Developer-owned synthetic probes; retained real MySQL, no acceptance/oracle inputs. */
@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class Wave5DeveloperIntegrationTest {
    @Autowired AuditQueryService auditQueries;@Autowired AuditWriteService auditWrites;@Autowired FailureAuditService auditFailures;@Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired EntityManager em;@Autowired UserAdministrationService administration;@Autowired UserAccessService access;@Autowired RequirementService requirements;@Autowired ReviewService reviews;@Autowired ChangeTransactions writes;@Autowired ChangeService changes;@Autowired PlatformTransactionManager manager;@Autowired VersionAppendService versions;@Autowired RequirementMutationService mutations;
    private static final String PASSWORD="Wave five developer é";
    private record Created(String username,UserData user) {}
    @AfterEach void cleanupContext() { org.springframework.security.test.context.TestSecurityContextHolder.clearContext(); }
    private long adminId() { return ((Number)em.createNativeQuery("SELECT MIN(u.userId) FROM `User` u JOIN `UserRole` ur ON ur.userId=u.userId JOIN `Role` r ON r.roleId=ur.roleId WHERE u.accountStatus='ENABLED' AND r.roleCode='ADMIN'").getSingleResult()).longValue(); }
    private void act(long id) { Actor actor=access.requireEnabledActor(id);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(actor,null,List.of())); }
    private Created user(String... roles) { act(adminId());String name="w5_"+UUID.randomUUID().toString().replace("-","");var result=new Created(name,administration.create(name,name+"@example.invalid","Wave five synthetic",PASSWORD,List.of(roles)));org.springframework.security.test.context.TestSecurityContextHolder.clearContext();return result; }
    private MockHttpSession login(Created u)throws Exception { org.springframework.security.test.context.TestSecurityContextHolder.clearContext();var csrf=mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();var session=(MockHttpSession)csrf.getRequest().getSession(false);mvc.perform(post("/api/v1/auth/login").session(session).header("X-CSRF-TOKEN",read(csrf).get("token").asText()).contentType("application/json").content(json.writeValueAsString(Map.of("login",u.username(),"password",PASSWORD)))).andExpect(status().isOk());return session; }
    private JsonNode read(MvcResult r)throws Exception { return json.readTree(r.getResponse().getContentAsString()); }
    private JsonNode postJson(String path,MockHttpSession s,Object body,int expected)throws Exception { org.springframework.security.test.context.TestSecurityContextHolder.clearContext();String csrf=read(mvc.perform(get("/api/v1/auth/csrf").session(s)).andExpect(status().isOk()).andReturn()).get("token").asText();return read(mvc.perform(post(path).session(s).header("X-CSRF-TOKEN",csrf).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().is(expected)).andReturn()); }
    private JsonNode patch(String path,MockHttpSession s,Object body,int expected)throws Exception { org.springframework.security.test.context.TestSecurityContextHolder.clearContext();String csrf=read(mvc.perform(get("/api/v1/auth/csrf").session(s)).andExpect(status().isOk()).andReturn()).get("token").asText();return read(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(path).session(s).header("X-CSRF-TOKEN",csrf).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().is(expected)).andReturn()); }
    private JsonNode getJson(String path,MockHttpSession s,int expected)throws Exception { org.springframework.security.test.context.TestSecurityContextHolder.clearContext();return read(mvc.perform(get(path).session(s)).andExpect(status().is(expected)).andReturn()); }
    private Map<String,Object> content(String title) { return Map.of("title",title,"description","Explicit observable behavior","level","USER","kind","FUNCTIONAL","priority","HIGH","source","Synthetic stakeholder interview","rationale","Prevent data loss","acceptanceCriteria","Given a valid form, saving returns its saved record with unchanged values."); }
    private JsonNode create(MockHttpSession s)throws Exception { return postJson("/api/v1/requirements",s,content("w5_"+UUID.randomUUID()),201); }
    private String path(JsonNode r) { return "/api/v1/requirements/"+r.get("requirementId").asText(); }
    private JsonNode submit(JsonNode r,MockHttpSession s)throws Exception { return postJson(path(r)+"/submit",s,Map.of("expectedLockVersion",r.get("lockVersion").asText()),201); }
    private JsonNode decision(JsonNode review,MockHttpSession s,String value,String revision,String comment,int expected)throws Exception { Map<String,Object> body=new HashMap<>();body.put("decision",value);body.put("expectedRequirementLockVersion",revision);body.put("comment",comment);return postJson("/api/v1/requirement-reviews/"+review.get("reviewId").asText()+"/decision",s,body,expected); }
    private long count(String table,String where,long id) { return ((Number)em.createNativeQuery("SELECT COUNT(*) FROM `"+table+"` WHERE "+where+"=:id").setParameter("id",id).getSingleResult()).longValue(); }
    private long auditCount(String target,String action,String outcome) { return ((Number)em.createNativeQuery("SELECT COUNT(*) FROM `AuditEvent` WHERE targetId=:id AND action=:action AND outcome=:outcome").setParameter("id",target).setParameter("action",action).setParameter("outcome",outcome).getSingleResult()).longValue(); }

    private JsonNode baseline(MockHttpSession eng,MockHttpSession rev)throws Exception { var r=create(eng);return decision(submit(r,eng),rev,"APPROVE","1",null,200).get("requirement"); }
    private String cp(JsonNode c) { return "/api/v1/changes/"+c.get("changeRequestId").asText(); }
    private JsonNode newChange(JsonNode r,MockHttpSession eng)throws Exception { return postJson(path(r)+"/changes",eng,Map.of("requestTitle","Explicit proposal","reason","Synthetic controlled change reason","expectedRequirementLockVersion",r.get("lockVersion").asText()),201); }
    private JsonNode crSubmit(JsonNode c,MockHttpSession eng)throws Exception { return postJson(cp(c)+"/submit",eng,Map.of("expectedLockVersion",c.get("lockVersion").asText()),201); }
    private JsonNode crDecision(JsonNode round,MockHttpSession rev,String value,String revision,String comment,int expected)throws Exception { var body=new HashMap<String,Object>();body.put("decision",value);body.put("comment",comment);body.put("expectedChangeLockVersion",revision);return postJson("/api/v1/change-reviews/"+round.get("changeReviewId").asText()+"/decision",rev,body,expected); }
    private JsonNode crApply(JsonNode c,JsonNode r,MockHttpSession eng,int expected)throws Exception { return postJson(cp(c)+"/apply",eng,Map.of("expectedRequirementLockVersion",r.get("lockVersion").asText(),"expectedChangeLockVersion",c.get("lockVersion").asText()),expected); }
    private static final List<String> CONTENT=List.of("Title","Description","Level","Kind","Priority","Source","Rationale","AcceptanceCriteria");
    private void snapshotEquals(JsonNode c,JsonNode round) { assertEquals(c.get("baseVersionId"),round.get("snapshotBaseVersionId"));assertEquals(c.get("requestTitle"),round.get("snapshotRequestTitle"));assertEquals(c.get("reason"),round.get("snapshotReason"));for(String field:CONTENT)assertEquals(c.get("proposed"+field),round.get("snapshotProposed"+field)); }

    private JsonNode query(String endpoint,MockHttpSession session,Map<String,String> parameters,int expected)throws Exception {
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();var request=get(endpoint).session(session);parameters.forEach(request::param);
        return read(mvc.perform(request).andExpect(status().is(expected)).andExpect(header().string("Cache-Control","no-store")).andReturn());
    }
    private Set<String> names(JsonNode node) { Set<String> names=new HashSet<>();node.fieldNames().forEachRemaining(names::add);return names; }
    private void safePage(JsonNode page) {
        assertEquals(Set.of("items","page","size","totalElements"),names(page));assertTrue(page.get("totalElements").isIntegralNumber());
        for(JsonNode a:page.get("items")) {
            assertEquals(Set.of("auditId","actorId","requirementId","targetType","targetId","action","outcome","beforeData","afterData","occurredAt"),names(a));
            assertTrue(a.get("auditId").isTextual());assertTrue(a.get("actorId").isTextual());assertTrue(a.get("requirementId").isNull() || a.get("requirementId").isTextual());
            assertTrue(a.get("occurredAt").asText().endsWith("Z"));assertEquals(0,Instant.parse(a.get("occurredAt").asText()).getNano()%1000);
            assertFalse(a.toString().contains("passwordHash"));assertFalse(a.toString().contains(PASSWORD));
        }
    }
    private long totalAudit() { return ((Number)em.createNativeQuery("SELECT COUNT(*) FROM AuditEvent").getSingleResult()).longValue(); }
    @Test void frozenPaginationRangeActionAndOriginalValuesAreReadOnly()throws Exception {
        var eng=login(user("REQUIREMENT_ENGINEER"));var admin=login(user("ADMIN"));String original="  Original synthetic title \n ";var r=postJson("/api/v1/requirements",eng,content(original),201);String p=path(r);
        var edited=patch(p+"/content",eng,Map.of("title","  Replacement title  ","expectedLockVersion","0"),200);
        patch(p+"/metadata",eng,Map.of("tagIds",List.of(),"expectedLockVersion","1"),200);
        var before=getJson(p,eng,200);long auditBefore=totalAudit();var all=query(p+"/history",eng,Map.of("size","100"),200);safePage(all);assertEquals(3,all.get("items").size());assertEquals(3,all.get("totalElements").asInt());
        JsonNode created=all.get("items").get(0),change=all.get("items").get(1);assertEquals("REQUIREMENT_CREATE",created.get("action").asText());assertEquals(original,created.get("afterData").get("title").asText());assertTrue(created.get("beforeData").isNull());assertEquals(original,change.get("beforeData").get("title").asText());assertEquals(edited.get("title"),change.get("afterData").get("title"));
        assertTrue(change.get("beforeData").get("lockVersion").isTextual());assertTrue(change.get("afterData").get("requirementId").isTextual());
        for(int i=0;i<3;i++) { var one=query(p+"/history",eng,Map.of("page",Integer.toString(i),"size","1"),200);safePage(one);assertEquals(3,one.get("totalElements").asInt());assertEquals(all.get("items").get(i),one.get("items").get(0)); }
        assertEquals(0,query(p+"/history",eng,Map.of("page","3","size","1"),200).get("items").size());
        String at=change.get("occurredAt").asText(),offset=Instant.parse(at).atOffset(ZoneOffset.ofHours(8)).toString();
        var bounded=query(p+"/history",eng,Map.of("from",offset,"to",at,"action","REQUIREMENT_CONTENT_EDIT"),200);assertEquals(1,bounded.get("items").size());assertEquals(change,bounded.get("items").get(0));
        var system=query("/api/v1/audit-events",admin,Map.of("from",offset,"to",at,"action","REQUIREMENT_CONTENT_EDIT"),200);safePage(system);assertEquals(1,system.get("items").size());assertEquals(change,system.get("items").get(0));
        assertEquals(0,query(p+"/history",eng,Map.of("action","x' OR 1=1 --"),200).get("totalElements").asInt());assertEquals(0,query(p+"/history",eng,Map.of("action",""),200).get("totalElements").asInt());
        assertEquals(before,getJson(p,eng,200));assertEquals(auditBefore,totalAudit());
        Object[] raw=(Object[])em.createNativeQuery("SELECT CAST(beforeData AS CHAR),CAST(afterData AS CHAR) FROM AuditEvent WHERE auditId=:id").setParameter("id",Long.parseLong(change.get("auditId").asText())).getSingleResult();assertEquals(json.readTree((String)raw[0]),change.get("beforeData"));assertEquals(json.readTree((String)raw[1]),change.get("afterData"));
    }
    @Test void everyEnabledRoleHistoryOnlyAdminSystemAndLiveRoles()throws Exception {
        var eng=login(user("REQUIREMENT_ENGINEER"));var r=create(eng);String history=path(r)+"/history";
        for(RoleCode role:RoleCode.values()) {
            var single=user(role.name());var session=login(single);safePage(getJson(history,session,200));getJson("/api/v1/audit-events",session,role==RoleCode.ADMIN?200:403);
            act(single.user().userId());if(role==RoleCode.ADMIN)assertDoesNotThrow(()->auditQueries.system(0,20,null,null,null));else assertEquals(ErrorCode.FORBIDDEN,assertThrows(RmsException.class,()->auditQueries.system(0,20,null,null,null)).code());
        }
        var union=user("ADMIN","VIEWER");var session=login(union);getJson("/api/v1/audit-events",session,200);act(adminId());administration.roles(Long.toString(union.user().userId()),Map.of("roles",List.of("VIEWER"),"expectedLockVersion","0"));getJson("/api/v1/audit-events",session,403);getJson(history,session,200);
        act(adminId());administration.enabled(Long.toString(union.user().userId()),"1",false);assertEquals("ACCOUNT_DISABLED",getJson(history,session,403).get("code").asText());
        var onlyAdmin=login(user("ADMIN"));postJson("/api/v1/requirements",onlyAdmin,content("Forbidden admin draft"),403);
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();mvc.perform(get(history)).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/audit-events")).andExpect(status().isUnauthorized());
    }
    @Test void nonexistentEmptyAndWithdrawnParentsHaveDistinctRetainedHistory()throws Exception {
        var engUser=user("REQUIREMENT_ENGINEER");var eng=login(engUser);var viewer=login(user("VIEWER"));var r=create(eng);
        var withdrawn=postJson(path(r)+"/withdraw",eng,Map.of("expectedLockVersion","0"),200);long countBefore=totalAudit();var history=getJson(path(r)+"/history",viewer,200);safePage(history);assertEquals(2,history.get("items").size());JsonNode event=history.get("items").get(1);assertEquals("REQUIREMENT_WITHDRAW",event.get("action").asText());assertEquals(0,event.get("beforeData").get("isWithdrawn").asInt());assertEquals(1,event.get("afterData").get("isWithdrawn").asInt());assertEquals(withdrawn.get("withdrawnBy"),event.get("afterData").get("withdrawnBy"));assertEquals(withdrawn.get("title"),event.get("afterData").get("title"));assertEquals(countBefore,totalAudit());
        assertEquals("NOT_FOUND",getJson("/api/v1/requirements/9223372036854775807/history",viewer,404).get("code").asText());
        act(engUser.user().userId());var tx=new TransactionTemplate(manager);long[] emptyId={0};tx.executeWithoutResult(status->{
            var empty=RequirementEntity.draft("D-"+UUID.randomUUID().toString().substring(0,20),"Empty developer parent","description","USER","FUNCTIONAL","HIGH",null,null,null,engUser.user().userId(),Instant.now());em.persist(empty);em.flush();emptyId[0]=empty.getRequirementId();
            var page=auditQueries.history(Long.toString(emptyId[0]),0,20,null,null,null);assertEquals(0,page.totalElements());assertTrue(page.items().isEmpty());status.setRollbackOnly();
        });assertEquals(0,count("Requirement","requirementId",emptyId[0]));
    }
    @Test void crossOwnerSuccessAuditReadsFormalVersionContextAndIndependentFailures()throws Exception {
        var engineer=user("REQUIREMENT_ENGINEER");var reviewer=user("REVIEWER");var eng=login(engineer);var rev=login(reviewer);var admin=login(user("ADMIN"));var member=login(user("PROJECT_MEMBER"));var r=baseline(eng,rev);
        var implementation=postJson(path(r)+"/implement",member,Map.of("expectedLockVersion",r.get("lockVersion").asText(),"expectedVersionId",r.get("currentVersionId").asText(),"description","  Original implementation evidence  "),200);
        var cr=newChange(implementation,eng);var submitted=crSubmit(cr,eng);var approved=crDecision(submitted,rev,"APPROVE","1",null,200).get("change");var applied=crApply(approved,implementation,eng,200);var parent=applied.get("requirement");
        var comment=postJson(path(parent)+"/comments",eng,Map.of("content","  Original comment  "),201);
        var parentDetail=getJson(path(parent),eng,200);long beforeRead=totalAudit();var history=query(path(parent)+"/history",eng,Map.of("size","100"),200);safePage(history);Set<String> types=new HashSet<>();for(JsonNode a:history.get("items"))types.add(a.get("targetType").asText());assertTrue(types.containsAll(Set.of("REQUIREMENT","REVIEW","CHANGE_REQUEST","CHANGE_REVIEW","COMMENT")));
        JsonNode initial=null,apply=null,implemented=null;for(JsonNode a:history.get("items")){if(a.get("action").asText().equals("REQUIREMENT_REVIEW_DECISION"))initial=a;if(a.get("action").asText().equals("CHANGE_APPLY"))apply=a;if(a.get("action").asText().equals("REQUIREMENT_IMPLEMENT"))implemented=a;}
        assertNotNull(initial);assertTrue(initial.get("beforeData").get("currentVersionId").isNull());assertEquals(r.get("currentVersionId"),initial.get("afterData").get("currentVersionId"));assertNotNull(apply);assertEquals(implementation.get("currentVersionId"),apply.get("beforeData").get("currentVersionId"));assertEquals(parent.get("currentVersionId"),apply.get("afterData").get("versionId"));assertEquals(2,apply.get("afterData").get("versionNo").asInt());assertEquals("APPROVED",apply.get("afterData").get("requirementStatus").asText());assertEquals(Long.toString(engineer.user().userId()),apply.get("actorId").asText());assertEquals("  Original implementation evidence  ",implemented.get("afterData").get("explanation").asText());
        assertEquals(2,getJson(path(parent)+"/versions",eng,200).get("items").size());assertEquals(beforeRead,totalAudit());
        String marker="W5_PROBE_"+UUID.randomUUID().toString().replace("-","").toUpperCase();var tx=new TransactionTemplate(manager);act(engineer.user().userId());
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{auditWrites.success(new AuditChange(Long.parseLong(parent.get("requirementId").asText()),"REQUIREMENT",parent.get("requirementId").asText(),marker,null,Map.of("title","Rolled back projection")));throw new IllegalStateException("DEVELOPER_ROLLBACK");}));
        assertEquals(0,query("/api/v1/audit-events",admin,Map.of("action",marker),200).get("totalElements").asInt());
        auditFailures.failed(access.requireEnabledActor(engineer.user().userId()),new AuditChange(null,"REQUIREMENT",parent.get("requirementId").asText(),marker,null,null));auditFailures.denied(access.requireEnabledActor(reviewer.user().userId()),new AuditChange(null,"REQUIREMENT",parent.get("requirementId").asText(),marker,null,null));
        var failed=query("/api/v1/audit-events",admin,Map.of("action",marker),200);safePage(failed);assertEquals(2,failed.get("totalElements").asInt());assertEquals(Set.of("FAILED","DENIED"),Set.of(failed.get("items").get(0).get("outcome").asText(),failed.get("items").get(1).get("outcome").asText()));assertEquals(parent.get("requirementId"),failed.get("items").get(0).get("requirementId"));assertEquals(parent.get("requirementId"),failed.get("items").get(1).get("requirementId"));assertEquals(parentDetail,getJson(path(parent),eng,200));
    }
    @Test void invalidQuerySafeErrorsAndStoredProjectionWhitelist()throws Exception {
        var engineer=user("REQUIREMENT_ENGINEER");var eng=login(engineer);var r=create(eng);String p=path(r)+"/history";long auditBefore=totalAudit();
        for(Map<String,String> params:List.of(Map.of("page","-1"),Map.of("size","0"),Map.of("size","101"),Map.of("page","abc"),Map.of("from","2026-10-03T12:00:00"),Map.of("to","not a date"),Map.of("from","2026-10-03T12:01:00Z","to","2026-10-03T12:00:00Z"),Map.of("action","x".repeat(65)))) {
            var error=query(p,eng,params,400);assertEquals(Set.of("code","message","correlationId"),names(error));assertEquals("INVALID_INPUT",error.get("code").asText());
        }
        getJson("/api/v1/requirements/01/history",eng,400);assertEquals(auditBefore,totalAudit());
        act(engineer.user().userId());var tx=new TransactionTemplate(manager);long[] unsafeId={0};tx.executeWithoutResult(status->{
            var unsafe=AuditEventEntity.append(engineer.user().userId(),Long.parseLong(r.get("requirementId").asText()),"REQUIREMENT",r.get("requirementId").asText(),"W5_UNSAFE_PROBE","SUCCESS",null,"{\"passwordHash\":\"developer unsafe sentinel\"}",Instant.now());em.persist(unsafe);em.flush();unsafeId[0]=unsafe.getAuditId();
            assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class,()->auditQueries.history(r.get("requirementId").asText(),0,20,null,null,"W5_UNSAFE_PROBE"));
            try { var error=query(p,eng,Map.of("action","W5_UNSAFE_PROBE"),500);assertEquals(Set.of("code","message","correlationId"),names(error));assertEquals("INTERNAL_ERROR",error.get("code").asText());assertFalse(error.toString().contains("developer unsafe sentinel"));assertFalse(error.toString().contains("passwordHash")); }
            catch(Exception e) { throw new AssertionError(e); }status.setRollbackOnly();
        });assertEquals(0,count("AuditEvent","auditId",unsafeId[0]));assertEquals(auditBefore,totalAudit());
    }
    private JsonNode deleteJson(String endpoint,MockHttpSession session,Map<String,String> parameters,int expected)throws Exception {
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();String csrf=read(mvc.perform(get("/api/v1/auth/csrf").session(session)).andExpect(status().isOk()).andReturn()).get("token").asText();var request=delete(endpoint).session(session).header("X-CSRF-TOKEN",csrf);parameters.forEach(request::param);return read(mvc.perform(request).andExpect(status().is(expected)).andReturn());
    }
    private void containsEvent(JsonNode history,String type,String target,String action,String outcome,String requirement) {
        boolean found=false;for(JsonNode a:history.get("items"))if(a.get("targetType").asText().equals(type) && a.get("targetId").asText().equals(target) && a.get("action").asText().equals(action) && a.get("outcome").asText().equals(outcome)){assertEquals(requirement,a.get("requirementId").asText());found=true;}
        assertTrue(found,()->"Missing requirement-associated "+type+" "+action+" "+outcome);
    }
    @Test void failedMainNestedAndWithdrawnContextsPreserveErrorsAndUnknownFk()throws Exception {
        var engineer=user("REQUIREMENT_ENGINEER");var eng=login(engineer);var viewer=login(user("VIEWER"));var admin=login(user("ADMIN"));var r=create(eng);String p=path(r),id=r.get("requirementId").asText();var unchanged=getJson(p,eng,200);
        assertEquals("LOCK_VERSION_CONFLICT",patch(p+"/content",eng,Map.of("title","Not saved","expectedLockVersion","99"),409).get("code").asText());
        patch(p+"/content",viewer,Map.of("title","Forbidden write","expectedLockVersion","0"),403);
        patch(p+"/content",eng,Map.of("title","Unknown write input","expectedLockVersion","0","secretProbe","NEVER_COPY_REQUEST_BODY"),400);
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();String malformedCsrf=read(mvc.perform(get("/api/v1/auth/csrf").session(eng)).andExpect(status().isOk()).andReturn()).get("token").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(p+"/content").session(eng).header("X-CSRF-TOKEN",malformedCsrf).contentType("application/json").content("{\"title\":\"NEVER_COPY_MALFORMED_BODY\"" )).andExpect(status().isBadRequest());
        postJson(p+"/comments",viewer,Map.of("content","Denied nested comment"),403);
        postJson(p+"/changes",viewer,Map.of("requestTitle","Denied nested CR","reason","Reason","expectedRequirementLockVersion","0"),403);
        postJson(p+"/comments",eng,Map.of("content"," "),400);postJson(p+"/changes",eng,Map.of("requestTitle","Draft has no formal base","reason","reason","expectedRequirementLockVersion","0"),409);
        assertEquals(unchanged,getJson(p,eng,200));var history=query(p+"/history",eng,Map.of("size","100"),200);safePage(history);
        containsEvent(history,"REQUIREMENT",id,"REQUIREMENT_CONTENT_EDIT","FAILED",id);containsEvent(history,"REQUIREMENT",id,"HTTP_WRITE_DENIED","DENIED",id);containsEvent(history,"REQUIREMENT",id,"HTTP_INPUT_FAILED","FAILED",id);
        containsEvent(history,"COMMENT","NEW","HTTP_WRITE_DENIED","DENIED",id);containsEvent(history,"CHANGE_REQUEST","NEW","HTTP_WRITE_DENIED","DENIED",id);containsEvent(history,"COMMENT","NEW","COMMENT_CREATE","FAILED",id);containsEvent(history,"CHANGE_REQUEST","NEW","CHANGE_CREATE","FAILED",id);assertFalse(history.toString().contains("NEVER_COPY_REQUEST_BODY"));assertFalse(history.toString().contains("NEVER_COPY_MALFORMED_BODY"));
        postJson(p+"/withdraw",eng,Map.of("expectedLockVersion","0"),200);var withdrawn=getJson(p,eng,200);patch(p+"/content",eng,Map.of("title","Not saved after withdrawal","expectedLockVersion","1"),409);var retained=query(p+"/history",viewer,Map.of("action","REQUIREMENT_CONTENT_EDIT"),200);assertEquals(3,retained.get("totalElements").asInt());assertEquals(withdrawn,getJson(p,eng,200));
        String absent="9223372036854775807";patch("/api/v1/requirements/"+absent+"/content",eng,Map.of("title","Missing target","expectedLockVersion","0"),404);patch("/api/v1/requirements/"+absent+"/content",viewer,Map.of("title","Missing denied target","expectedLockVersion","0"),403);patch("/api/v1/requirements/9223372036854775808/content",eng,Map.of("title","Out of signed range","expectedLockVersion","0"),400);patch("/api/v1/requirements/01/content",eng,Map.of("title","Noncanonical target","expectedLockVersion","0"),400);
        assertEquals(0,((Number)em.createNativeQuery("SELECT COUNT(*) FROM AuditEvent WHERE targetId=:id AND requirementId IS NOT NULL").setParameter("id",absent).getSingleResult()).intValue());
        long count=totalAudit();org.springframework.security.test.context.TestSecurityContextHolder.clearContext();mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(p+"/content").contentType("application/json").content("{}" )).andExpect(status().isForbidden());assertEquals(count,totalAudit());
        var system=getJson("/api/v1/audit-events",admin,200);safePage(system);assertEquals(1,count("Requirement","requirementId",Long.parseLong(id)));
    }
    @Test void allExistingChildTargetsLocateOwnParentAndMissingChildrenRemainSystemOnly()throws Exception {
        var engineer=user("REQUIREMENT_ENGINEER","REVIEWER");var eng=login(engineer);var reviewer=login(user("REVIEWER"));var viewer=login(user("VIEWER"));var member=login(user("PROJECT_MEMBER"));var r=create(eng);var round=submit(r,eng);
        assertEquals("SELF_REVIEW",decision(round,eng,"APPROVE","1",null,403).get("code").asText());r=decision(round,reviewer,"APPROVE","1",null,200).get("requirement");String id=r.get("requirementId").asText(),p=path(r);
        var cr=newChange(r,eng);patch(cp(cr),viewer,Map.of("requestTitle","Denied CR edit","expectedLockVersion","0"),403);var crRound=crSubmit(cr,eng);crDecision(crRound,eng,"APPROVE","1",null,403);patch(cp(cr),eng,Map.of("requestTitle","Submitted cannot edit","expectedLockVersion","1"),409);var approved=crDecision(crRound,reviewer,"APPROVE","1",null,200).get("change");var applied=crApply(approved,r,eng,200);var parent=applied.get("requirement");crApply(applied.get("change"),parent,eng,409);
        var comment=postJson(p+"/comments",eng,Map.of("content","Retained own comment"),201);assertEquals("NOT_AUTHOR",deleteJson("/api/v1/comments/"+comment.get("commentId").asText(),member,Map.of(),403).get("code").asText());deleteJson("/api/v1/comments/"+comment.get("commentId").asText(),viewer,Map.of(),403);
        var other=create(eng);var relation=postJson("/api/v1/relations",eng,Map.of("sourceRequirementId",other.get("requirementId").asText(),"targetRequirementId",id,"relationType","RELATES_TO","expectedSourceLockVersion","0","expectedTargetLockVersion",parent.get("lockVersion").asText()),201);assertEquals(id,relation.get("sourceRequirementId").asText());String relationId=relation.get("relationId").asText();
        deleteJson("/api/v1/relations/"+relationId,viewer,Map.of("expectedSourceLockVersion",parent.get("lockVersion").asText(),"expectedTargetLockVersion","0"),403);deleteJson("/api/v1/relations/"+relationId,eng,Map.of("expectedSourceLockVersion","99","expectedTargetLockVersion","0"),409);
        var detail=getJson(p,eng,200);var history=query(p+"/history",eng,Map.of("size","100"),200);safePage(history);
        containsEvent(history,"REVIEW",round.get("reviewId").asText(),"REQUIREMENT_REVIEW_DECISION","DENIED",id);containsEvent(history,"CHANGE_REQUEST",cr.get("changeRequestId").asText(),"HTTP_WRITE_DENIED","DENIED",id);containsEvent(history,"CHANGE_REVIEW",crRound.get("changeReviewId").asText(),"CHANGE_REVIEW_DECISION","DENIED",id);containsEvent(history,"CHANGE_REQUEST",cr.get("changeRequestId").asText(),"CHANGE_EDIT","FAILED",id);containsEvent(history,"CHANGE_REQUEST",cr.get("changeRequestId").asText(),"CHANGE_APPLY","FAILED",id);
        containsEvent(history,"COMMENT",comment.get("commentId").asText(),"COMMENT_DELETE","DENIED",id);containsEvent(history,"COMMENT",comment.get("commentId").asText(),"HTTP_WRITE_DENIED","DENIED",id);containsEvent(history,"RELATION",relationId,"HTTP_WRITE_DENIED","DENIED",id);containsEvent(history,"RELATION",relationId,"RELATION_REMOVE","FAILED",id);
        assertEquals(detail,getJson(p,eng,200));assertEquals(1,count("RequirementRelation","relationId",Long.parseLong(relationId)));
        String absent="9223372036854775807";postJson("/api/v1/requirement-reviews/"+absent+"/decision",reviewer,Map.of("decision","APPROVE","expectedRequirementLockVersion","0"),404);patch("/api/v1/changes/"+absent,eng,Map.of("requestTitle","Missing CR","expectedLockVersion","0"),404);postJson("/api/v1/change-reviews/"+absent+"/decision",reviewer,Map.of("decision","APPROVE","expectedChangeLockVersion","0"),404);deleteJson("/api/v1/comments/"+absent,eng,Map.of(),404);deleteJson("/api/v1/relations/"+absent,eng,Map.of("expectedSourceLockVersion","0","expectedTargetLockVersion","0"),404);
        assertEquals(0,((Number)em.createNativeQuery("SELECT COUNT(*) FROM AuditEvent WHERE targetId=:id AND requirementId IS NOT NULL").setParameter("id",absent).getSingleResult()).intValue());
    }

}

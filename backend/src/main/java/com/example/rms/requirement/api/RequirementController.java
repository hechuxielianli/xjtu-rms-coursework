package com.example.rms.requirement.api;
import com.example.rms.requirement.application.*;
import com.example.rms.shared.api.PageResponse;
import com.example.rms.user.application.contract.UserSummaryData;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class RequirementController {
    public record CreateRequest(String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria) {}
    public record CreateTagRequest(String name,String description) {}
    public record LockCommand(String expectedLockVersion) {}
    public record VersionConfirmationRequest(String expectedVersionId,String expectedLockVersion,String description) {}
    public record SummaryResponse(String userId,String displayName,String accountStatus) {}
    public record TagResponse(String tagId,String name,String description,String createdBy,Instant createdAt) {}
    public record RequirementResponse(String requirementId,String requirementKey,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,String status,String creatorId,String assigneeId,String currentVersionId,Instant firstSubmittedAt,byte isWithdrawn,String withdrawnBy,Instant withdrawnAt,Instant createdAt,Instant updatedAt,String lockVersion,SummaryResponse creator,SummaryResponse assignee,List<TagResponse> tags) {}
    private final RequirementService service;
    public RequirementController(RequirementService service) { this.service=service; }
    @GetMapping("/requirements") public ResponseEntity<PageResponse<RequirementResponse>> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String keyword,@RequestParam(required=false) String status,@RequestParam(required=false) String kind,@RequestParam(required=false) String level,@RequestParam(required=false) String priority,@RequestParam(required=false) String tag) { return ok(PageResponse.from(service.list(page,size,keyword,status,kind,level,priority,tag),RequirementController::response)); }
    @GetMapping("/requirements/{id}") public ResponseEntity<RequirementResponse> read(@PathVariable String id) { return ok(response(service.read(id))); }
    @PostMapping("/requirements") public ResponseEntity<RequirementResponse> create(@RequestBody CreateRequest request) { return created(response(service.create(request.title(),request.description(),request.level(),request.kind(),request.priority(),request.source(),request.rationale(),request.acceptanceCriteria()))); }
    @PatchMapping("/requirements/{id}/content") public ResponseEntity<RequirementResponse> content(@PathVariable String id,@RequestBody Map<String,Object> fields) { return ok(response(service.content(id,fields))); }
    @PatchMapping("/requirements/{id}/metadata") public ResponseEntity<RequirementResponse> metadata(@PathVariable String id,@RequestBody Map<String,Object> fields) { return ok(response(service.metadata(id,fields))); }
    @PostMapping("/requirements/{id}/withdraw") public ResponseEntity<RequirementResponse> withdraw(@PathVariable String id,@RequestBody LockCommand request) { return ok(response(service.withdraw(id,request.expectedLockVersion()))); }
    @PostMapping("/requirements/{id}/reopen") public ResponseEntity<RequirementResponse> reopen(@PathVariable String id,@RequestBody LockCommand request) { return ok(response(service.reopen(id,request.expectedLockVersion()))); }
    @PostMapping("/requirements/{id}/implement") public ResponseEntity<RequirementResponse> implement(@PathVariable String id,@RequestBody VersionConfirmationRequest request) { return ok(response(service.confirm(id,request.expectedVersionId(),request.expectedLockVersion(),request.description(),false))); }
    @PostMapping("/requirements/{id}/verify") public ResponseEntity<RequirementResponse> verify(@PathVariable String id,@RequestBody VersionConfirmationRequest request) { return ok(response(service.confirm(id,request.expectedVersionId(),request.expectedLockVersion(),request.description(),true))); }
    @GetMapping("/tags") public ResponseEntity<PageResponse<TagResponse>> tags(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String name) { return ok(PageResponse.from(service.tags(page,size,name),RequirementController::tag)); }
    @PostMapping("/tags") public ResponseEntity<TagResponse> createTag(@RequestBody CreateTagRequest request) { return created(tag(service.createTag(request.name(),request.description()))); }
    @PatchMapping("/tags/{id}") public ResponseEntity<TagResponse> editTag(@PathVariable String id,@RequestBody Map<String,Object> fields) { return ok(tag(service.editTag(id,fields))); }
    private static RequirementResponse response(RequirementService.Detail detail) { var r=detail.requirement();return new RequirementResponse(decimal(r.requirementId()),r.requirementKey(),r.title(),r.description(),r.level(),r.kind(),r.priority(),r.source(),r.rationale(),r.acceptanceCriteria(),r.status(),decimal(r.creatorId()),decimal(r.assigneeId()),decimal(r.currentVersionId()),r.firstSubmittedAt(),r.isWithdrawn(),decimal(r.withdrawnBy()),r.withdrawnAt(),r.createdAt(),r.updatedAt(),decimal(r.lockVersion()),summary(detail.creator()),summary(detail.assignee()),r.tags().stream().map(RequirementController::tag).toList()); }
    private static SummaryResponse summary(UserSummaryData user) { return user==null?null:new SummaryResponse(decimal(user.userId()),user.displayName(),user.accountStatus()); }
    private static TagResponse tag(TagData t) { return new TagResponse(decimal(t.tagId()),t.name(),t.description(),decimal(t.createdBy()),t.createdAt()); }
    private static String decimal(Long id) { return id==null?null:Long.toString(id); }
    private static <T>ResponseEntity<T> ok(T body) { return ResponseEntity.ok().header("Cache-Control","no-store").body(body); }
    private static <T>ResponseEntity<T> created(T body) { return ResponseEntity.status(201).header("Cache-Control","no-store").body(body); }
}

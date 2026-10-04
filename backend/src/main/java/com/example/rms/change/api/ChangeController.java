package com.example.rms.change.api;
import com.example.rms.change.application.*;
import com.example.rms.requirement.application.contract.RequirementView;
import com.example.rms.version.application.contract.VersionData;
import com.example.rms.shared.api.PageResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class ChangeController {
    public record CreateRequest(String requestTitle,String reason,String expectedRequirementLockVersion) {}
    public record LockCommand(String expectedLockVersion) {}
    public record DecisionRequest(String decision,String comment,String expectedChangeLockVersion) {}
    public record ApplyRequest(String expectedRequirementLockVersion,String expectedChangeLockVersion) {}
    public record ChangeResponse(long changeRequestId,long requirementId,long baseVersionId,String requestTitle,String reason,String proposedTitle,String proposedDescription,String proposedLevel,String proposedKind,String proposedPriority,String proposedSource,String proposedRationale,String proposedAcceptanceCriteria,String status,long createdBy,Instant createdAt,Instant updatedAt,Long appliedBy,Instant appliedAt,long lockVersion) {}
    public record ReviewResponse(long changeReviewId,long requirementId,long changeRequestId,int roundNo,long snapshotBaseVersionId,String snapshotRequestTitle,String snapshotReason,String snapshotProposedTitle,String snapshotProposedDescription,String snapshotProposedLevel,String snapshotProposedKind,String snapshotProposedPriority,String snapshotProposedSource,String snapshotProposedRationale,String snapshotProposedAcceptanceCriteria,long submittedBy,Instant submittedAt,String reviewStatus,Long reviewerId,String decision,String comment,Instant decidedAt) {}
    public record VersionResponse(long versionId,long requirementId,int versionNo,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,Long initialReviewId,Long appliedChangeRequestId,long createdBy,Instant createdAt,String changeReason) {}
    public record DecisionResponse(ReviewResponse review,ChangeResponse change) {}
    public record ApplyResponse(ChangeResponse change,RequirementView requirement,VersionResponse version) {}
    private final ChangeService service;
    public ChangeController(ChangeService service) { this.service=service; }
    @PostMapping("/requirements/{id}/changes") public ResponseEntity<ChangeResponse> create(@PathVariable String id,@RequestBody CreateRequest request) { return ResponseEntity.status(201).header("Cache-Control","no-store").body(response(service.create(id,request.requestTitle(),request.reason(),request.expectedRequirementLockVersion()))); }
    @GetMapping("/requirements/{id}/changes") public ResponseEntity<PageResponse<ChangeResponse>> list(@PathVariable String id,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ok(PageResponse.from(service.list(id,status,page,size),ChangeController::response)); }
    @GetMapping("/changes/{id}") public ResponseEntity<ChangeResponse> read(@PathVariable String id) { return ok(response(service.read(id))); }
    @PatchMapping("/changes/{id}") public ResponseEntity<ChangeResponse> edit(@PathVariable String id,@RequestBody Map<String,Object> fields) { return ok(response(service.edit(id,fields))); }
    @PostMapping("/changes/{id}/submit") public ResponseEntity<ReviewResponse> submit(@PathVariable String id,@RequestBody LockCommand request) { return ResponseEntity.status(201).header("Cache-Control","no-store").body(response(service.submit(id,request.expectedLockVersion()))); }
    @PostMapping("/changes/{id}/cancel") public ResponseEntity<ChangeResponse> cancel(@PathVariable String id,@RequestBody LockCommand request) { return ok(response(service.cancel(id,request.expectedLockVersion()))); }
    @GetMapping("/changes/{id}/reviews") public ResponseEntity<PageResponse<ReviewResponse>> history(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ok(PageResponse.from(service.history(id,page,size),ChangeController::response)); }
    @GetMapping("/change-reviews/pending") public ResponseEntity<PageResponse<ReviewResponse>> pending(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ok(PageResponse.from(service.pending(page,size),ChangeController::response)); }
    @PostMapping("/change-reviews/{id}/decision") public ResponseEntity<DecisionResponse> decide(@PathVariable String id,@RequestBody DecisionRequest request) { var result=service.decide(id,request.decision(),request.comment(),request.expectedChangeLockVersion());return ok(new DecisionResponse(response(result.review()),response(result.change()))); }
    @PostMapping("/changes/{id}/apply") public ResponseEntity<ApplyResponse> apply(@PathVariable String id,@RequestBody ApplyRequest request) { var result=service.apply(id,request.expectedRequirementLockVersion(),request.expectedChangeLockVersion());return ok(new ApplyResponse(response(result.change()),result.requirement(),response(result.version()))); }
    private static ChangeResponse response(ChangeData r) { var c=r.proposed();return new ChangeResponse(r.changeRequestId(),r.requirementId(),r.baseVersionId(),r.requestTitle(),r.reason(),c.title(),c.description(),c.level(),c.kind(),c.priority(),c.source(),c.rationale(),c.acceptanceCriteria(),r.status(),r.createdBy(),r.createdAt(),r.updatedAt(),r.appliedBy(),r.appliedAt(),r.lockVersion()); }
    private static ReviewResponse response(ChangeReviewData r) { var c=r.proposed();return new ReviewResponse(r.changeReviewId(),r.requirementId(),r.changeRequestId(),r.roundNo(),r.snapshotBaseVersionId(),r.snapshotRequestTitle(),r.snapshotReason(),c.title(),c.description(),c.level(),c.kind(),c.priority(),c.source(),c.rationale(),c.acceptanceCriteria(),r.submittedBy(),r.submittedAt(),r.reviewStatus(),r.reviewerId(),r.decision(),r.comment(),r.decidedAt()); }
    private static VersionResponse response(VersionData v) { var c=v.content();return new VersionResponse(v.versionId(),v.requirementId(),v.versionNo(),c.title(),c.description(),c.level(),c.kind(),c.priority(),c.source(),c.rationale(),c.acceptanceCriteria(),v.initialReviewId(),v.appliedChangeRequestId(),v.createdBy(),v.createdAt(),v.changeReason()); }
    private static <T>ResponseEntity<T> ok(T body) { return ResponseEntity.ok().header("Cache-Control","no-store").body(body); }
}

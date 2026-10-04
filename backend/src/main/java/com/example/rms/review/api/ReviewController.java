package com.example.rms.review.api;
import com.example.rms.review.application.*;
import com.example.rms.requirement.application.contract.RequirementView;
import com.example.rms.version.application.contract.VersionData;
import com.example.rms.shared.api.PageResponse;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class ReviewController {
    public record LockCommand(String expectedLockVersion) {}
    public record DecisionRequest(String decision,String comment,String expectedRequirementLockVersion) {}
    public record ReviewResponse(long reviewId,long requirementId,int roundNo,String snapshotTitle,String snapshotDescription,String snapshotLevel,String snapshotKind,String snapshotPriority,String snapshotSource,String snapshotRationale,String snapshotAcceptanceCriteria,long submittedBy,Instant submittedAt,String reviewStatus,Long reviewerId,String decision,String comment,Instant decidedAt) {}
    public record VersionResponse(long versionId,long requirementId,int versionNo,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,Long initialReviewId,Long appliedChangeRequestId,long createdBy,Instant createdAt,String changeReason) {}
    public record DecisionResponse(ReviewResponse review,RequirementView requirement,VersionResponse version) {}
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service=service; }
    @PostMapping("/requirements/{id}/submit") public ResponseEntity<ReviewResponse> submit(@PathVariable String id,@RequestBody LockCommand request) { return ResponseEntity.status(201).header("Cache-Control","no-store").body(response(service.submit(id,request.expectedLockVersion()))); }
    @PostMapping("/requirement-reviews/{id}/decision") public ResponseEntity<DecisionResponse> decide(@PathVariable String id,@RequestBody DecisionRequest request) { var result=service.decide(id,request.decision(),request.comment(),request.expectedRequirementLockVersion());return ok(new DecisionResponse(response(result.review()),result.requirement(),version(result.version()))); }
    @GetMapping("/reviews/pending") public ResponseEntity<PageResponse<ReviewResponse>> pending(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ok(PageResponse.from(service.pending(page,size),ReviewController::response)); }
    @GetMapping("/requirements/{id}/reviews") public ResponseEntity<PageResponse<ReviewResponse>> history(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ok(PageResponse.from(service.history(id,page,size),ReviewController::response)); }
    private static ReviewResponse response(ReviewData r) { var c=r.content();return new ReviewResponse(r.reviewId(),r.requirementId(),r.roundNo(),c.title(),c.description(),c.level(),c.kind(),c.priority(),c.source(),c.rationale(),c.acceptanceCriteria(),r.submittedBy(),r.submittedAt(),r.reviewStatus(),r.reviewerId(),r.decision(),r.comment(),r.decidedAt()); }
    private static VersionResponse version(VersionData v) { if(v==null)return null;var c=v.content();return new VersionResponse(v.versionId(),v.requirementId(),v.versionNo(),c.title(),c.description(),c.level(),c.kind(),c.priority(),c.source(),c.rationale(),c.acceptanceCriteria(),v.initialReviewId(),v.appliedChangeRequestId(),v.createdBy(),v.createdAt(),v.changeReason()); }
    private static <T>ResponseEntity<T> ok(T body) { return ResponseEntity.ok().header("Cache-Control","no-store").body(body); }
}

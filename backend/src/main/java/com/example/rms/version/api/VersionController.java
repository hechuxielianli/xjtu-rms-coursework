package com.example.rms.version.api;
import com.example.rms.version.application.VersionQueryService;
import com.example.rms.version.application.contract.VersionData;
import com.example.rms.shared.api.PageResponse;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/requirements/{id}/versions")
public class VersionController {
    public record VersionResponse(long versionId,long requirementId,int versionNo,String title,String description,String level,String kind,String priority,String source,String rationale,String acceptanceCriteria,Long initialReviewId,Long appliedChangeRequestId,long createdBy,Instant createdAt,String changeReason) {}
    private final VersionQueryService service;
    public VersionController(VersionQueryService service) { this.service=service; }
    @GetMapping public ResponseEntity<PageResponse<VersionResponse>> list(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ResponseEntity.ok().header("Cache-Control","no-store").body(PageResponse.from(service.list(id,page,size),VersionController::response)); }
    @GetMapping("/{versionId}") public ResponseEntity<VersionResponse> read(@PathVariable String id,@PathVariable String versionId) { return ResponseEntity.ok().header("Cache-Control","no-store").body(response(service.read(id,versionId))); }
    private static VersionResponse response(VersionData v) { var c=v.content();return new VersionResponse(v.versionId(),v.requirementId(),v.versionNo(),c.title(),c.description(),c.level(),c.kind(),c.priority(),c.source(),c.rationale(),c.acceptanceCriteria(),v.initialReviewId(),v.appliedChangeRequestId(),v.createdBy(),v.createdAt(),v.changeReason()); }
}

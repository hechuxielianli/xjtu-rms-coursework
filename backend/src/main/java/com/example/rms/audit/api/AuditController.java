package com.example.rms.audit.api;
import com.example.rms.audit.application.*;
import com.example.rms.shared.api.PageResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class AuditController {
    public record AuditResponse(long auditId,long actorId,Long requirementId,String targetType,String targetId,String action,String outcome,Map<String,Object> beforeData,Map<String,Object> afterData,Instant occurredAt) {}
    private final AuditQueryService service;
    public AuditController(AuditQueryService service) { this.service=service; }
    @GetMapping("/requirements/{id}/history") public ResponseEntity<PageResponse<AuditResponse>> history(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String from,@RequestParam(required=false) String to,@RequestParam(required=false) String action) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(PageResponse.from(service.history(id,page,size,from,to,action),AuditController::response));
    }
    @GetMapping("/audit-events") public ResponseEntity<PageResponse<AuditResponse>> system(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String from,@RequestParam(required=false) String to,@RequestParam(required=false) String action) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(PageResponse.from(service.system(page,size,from,to,action),AuditController::response));
    }
    private static AuditResponse response(AuditData a) { return new AuditResponse(a.auditId(),a.actorId(),a.requirementId(),a.targetType(),a.targetId(),a.action(),a.outcome(),a.beforeData(),a.afterData(),a.occurredAt()); }
}

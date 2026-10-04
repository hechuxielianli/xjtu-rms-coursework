package com.example.rms.relation.api;
import com.example.rms.relation.application.*;
import com.example.rms.requirement.application.contract.RequirementView;
import com.example.rms.shared.api.PageResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class RelationController {
    public record CreateRequest(String sourceRequirementId,String targetRequirementId,String relationType,String description,String expectedSourceLockVersion,String expectedTargetLockVersion){}
    public record RelationResponse(long relationId,long sourceRequirementId,long targetRequirementId,String relationType,String description,long createdBy,Instant createdAt){}
    public record TraceResponse(long rootRequirementId,List<RequirementView> nodes,List<RelationResponse> edges,int depth,boolean truncated){}
    private final RelationService service;public RelationController(RelationService service){this.service=service;}
    @PostMapping("/relations") public ResponseEntity<RelationResponse> create(@RequestBody CreateRequest r){return ResponseEntity.status(201).header("Cache-Control","no-store").body(response(service.create(r.sourceRequirementId(),r.targetRequirementId(),r.relationType(),r.description(),r.expectedSourceLockVersion(),r.expectedTargetLockVersion())));}
    @DeleteMapping("/relations/{id}") public ResponseEntity<Void> remove(@PathVariable String id,@RequestParam String expectedSourceLockVersion,@RequestParam String expectedTargetLockVersion){service.remove(id,expectedSourceLockVersion,expectedTargetLockVersion);return ResponseEntity.noContent().header("Cache-Control","no-store").build();}
    @GetMapping("/requirements/{id}/relations") public ResponseEntity<PageResponse<RelationResponse>> list(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(PageResponse.from(service.list(id,page,size),RelationController::response));}
    @GetMapping("/requirements/{id}/trace") public ResponseEntity<TraceResponse> trace(@PathVariable String id,@RequestParam(defaultValue="2") int depth,@RequestParam(required=false) String relationType){var r=service.trace(id,depth,relationType);return ok(new TraceResponse(r.rootRequirementId(),r.nodes(),r.edges().stream().map(RelationController::response).toList(),r.depth(),r.truncated()));}
    private static RelationResponse response(RelationData r){return new RelationResponse(r.relationId(),r.sourceRequirementId(),r.targetRequirementId(),r.relationType(),r.description(),r.createdBy(),r.createdAt());}
    private static <T>ResponseEntity<T> ok(T body){return ResponseEntity.ok().header("Cache-Control","no-store").body(body);}
}

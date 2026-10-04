package com.example.rms.comment.api;
import com.example.rms.comment.application.*;
import com.example.rms.shared.api.PageResponse;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class CommentController {
    public record CreateRequest(String content) {}
    public record CommentResponse(String commentId,String requirementId,String authorId,String content,Instant createdAt,byte isDeleted,String deletedBy,Instant deletedAt) {}
    private final CommentService service;
    public CommentController(CommentService service) { this.service=service; }
    @GetMapping("/requirements/{id}/comments") public ResponseEntity<PageResponse<CommentResponse>> list(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ResponseEntity.ok().header("Cache-Control","no-store").body(PageResponse.from(service.list(id,page,size),CommentController::response)); }
    @PostMapping("/requirements/{id}/comments") public ResponseEntity<CommentResponse> create(@PathVariable String id,@RequestBody CreateRequest request) { return ResponseEntity.status(201).header("Cache-Control","no-store").body(response(service.create(id,request.content()))); }
    @DeleteMapping("/comments/{id}") public ResponseEntity<Void> delete(@PathVariable String id) { service.delete(id);return ResponseEntity.noContent().header("Cache-Control","no-store").build(); }
    private static CommentResponse response(CommentData c) { return new CommentResponse(Long.toString(c.commentId()),Long.toString(c.requirementId()),Long.toString(c.authorId()),c.content(),c.createdAt(),c.isDeleted(),c.deletedBy()==null?null:Long.toString(c.deletedBy()),c.deletedAt()); }
}

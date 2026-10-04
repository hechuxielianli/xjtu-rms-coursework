package com.example.rms.user.api;
import com.example.rms.user.application.*;
import com.example.rms.shared.api.PageResponse;
import com.example.rms.shared.domain.RoleCode;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/users")
public class UserController {
    public record CreateRequest(String username,String email,String displayName,String initialPassword,List<String> roles) { @Override public String toString(){return "CreateRequest[REDACTED]";} }
    public record LockRequest(String expectedLockVersion) {}
    public record AssignmentResponse(String userId,String roleId,String grantedBy,Instant grantedAt) {}
    public record UserResponse(String userId,String username,String email,String displayName,String accountStatus,Instant createdAt,Instant updatedAt,String lockVersion,List<RoleCode> roles,List<AssignmentResponse> assignments) {}
    private final UserAdministrationService service;
    public UserController(UserAdministrationService service) { this.service=service; }
    @GetMapping public ResponseEntity<PageResponse<UserResponse>> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ok(PageResponse.from(service.list(page,size),UserController::response)); }
    @PostMapping public ResponseEntity<UserResponse> create(@RequestBody CreateRequest request) { return ResponseEntity.status(201).header("Cache-Control","no-store").body(response(service.create(request.username(),request.email(),request.displayName(),request.initialPassword(),request.roles()))); }
    @PatchMapping("/{id}") public ResponseEntity<UserResponse> edit(@PathVariable String id,@RequestBody Map<String,Object> fields) { return ok(response(service.edit(id,fields))); }
    @PutMapping("/{id}/roles") public ResponseEntity<UserResponse> roles(@PathVariable String id,@RequestBody Map<String,Object> fields) { return ok(response(service.roles(id,fields))); }
    @PostMapping("/{id}/enable") public ResponseEntity<UserResponse> enable(@PathVariable String id,@RequestBody LockRequest request) { return ok(response(service.enabled(id,request.expectedLockVersion(),true))); }
    @PostMapping("/{id}/disable") public ResponseEntity<UserResponse> disable(@PathVariable String id,@RequestBody LockRequest request) { return ok(response(service.enabled(id,request.expectedLockVersion(),false))); }
    private static UserResponse response(UserData u) { return new UserResponse(Long.toString(u.userId()),u.username(),u.email(),u.displayName(),u.accountStatus(),u.createdAt(),u.updatedAt(),Long.toString(u.lockVersion()),u.roles(),u.assignments().stream().map(a->new AssignmentResponse(Long.toString(a.userId()),Long.toString(a.roleId()),Long.toString(a.grantedBy()),a.grantedAt())).toList()); }
    private static <T> ResponseEntity<T> ok(T body) { return ResponseEntity.ok().header("Cache-Control","no-store").body(body); }
}

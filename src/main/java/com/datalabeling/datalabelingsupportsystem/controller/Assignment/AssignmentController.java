package com.datalabeling.datalabelingsupportsystem.controller.Assignment;

import com.datalabeling.datalabelingsupportsystem.dto.request.Assignment.CreateAssignmentRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Assignment.AssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.service.Assignment.AssignmentService;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final UserRepository userRepository;

    /**
     * Extract userId từ UserDetails (lấy từ username hoặc principal)
     */
    private Long extractUserId(UserDetails userDetails) {
        String username = userDetails.getUsername();
        // Giả sử username là email hoặc userId dạng string, tìm User theo username
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return user.get().getUserId();
        }
        // Nếu không tìm được, thử parse username thành Long (nếu là userId)
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Không thể xác định userId từ UserDetails");
        }
    }

    /**
     * Tạo phân công - chỉ MANAGER
     */
    @PostMapping("/projects/{projectId}/assignments")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateAssignmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = extractUserId(userDetails);
        AssignmentResponse response = assignmentService.createAssignment(projectId, request, managerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Xem danh sách phân công - chỉ MANAGER
     */
    @GetMapping("/projects/{projectId}/assignments")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = extractUserId(userDetails);
        List<AssignmentResponse> list = assignmentService.getAssignmentsByProject(projectId, managerId);
        return ResponseEntity.ok(list);
    }

    /**
     * Xóa phân công (chỉ khi PENDING) - chỉ MANAGER
     */
    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = extractUserId(userDetails);
        assignmentService.deleteAssignment(assignmentId, managerId);
        return ResponseEntity.noContent().build();
    }
}

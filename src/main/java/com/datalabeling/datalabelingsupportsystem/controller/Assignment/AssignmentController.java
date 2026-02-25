package com.datalabeling.datalabelingsupportsystem.controller.Assignment;

import com.datalabeling.datalabelingsupportsystem.dto.request.Assignment.CreateAssignmentRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Assignment.AssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.service.Assignment.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Tạo phân công - chỉ MANAGER
     */
    @PostMapping("/projects/{projectId}/assignments")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateAssignmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = ((User) userDetails).getUserId(); // cast về User lấy userId
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

        Long managerId = ((User) userDetails).getUserId();
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

        Long managerId = ((User) userDetails).getUserId();
        assignmentService.deleteAssignment(assignmentId, managerId);
        return ResponseEntity.noContent().build();
    }
}

package com.datalabeling.datalabelingsupportsystem.controller.User;

import com.datalabeling.datalabelingsupportsystem.dto.request.User.CreateUserRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.User.UpdateUserRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.UserResponse;
import com.datalabeling.datalabelingsupportsystem.service.User.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    // CHỈ ADMIN - Tạo user mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new user (ADMIN only)", description = "Admin creates user and assigns role")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    // TẤT CẢ ROLE - Xem profile của chính mình
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Works for all roles: ADMIN, MANAGER, ANNOTATOR, REVIEWER")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    // TẤT CẢ ROLE - Update profile của chính mình
    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserResponse> updateCurrentUser(@RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(request));
    }

    // CHỈ ADMIN - Xem tất cả users
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get all users (ADMIN only)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    // CHỈ ADMIN - Xem user cụ thể
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (ADMIN only)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // ADMIN hoặc chính user đó - Update user
    @PutMapping("/{userId}")
    @Operation(summary = "Update user (ADMIN or self)")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    // CHỈ ADMIN - Lấy danh sách users chờ duyệt
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending users waiting for approval (ADMIN only)")
    public ResponseEntity<Page<UserResponse>> getPendingUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getPendingUsers(page, size));
    }

    // CHỈ ADMIN - Approve user (PENDING → ACTIVE)
    @PatchMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve pending user (ADMIN only)", description = "Change user status from PENDING to ACTIVE, allowing them to login")
    public ResponseEntity<UserResponse> approveUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.approveUser(userId));
    }

    // CHỈ ADMIN - Reject user (PENDING → REJECTED)
    @PatchMapping("/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject pending user (ADMIN only)")
    public ResponseEntity<UserResponse> rejectUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(userService.rejectUser(userId, reason));
    }

    // CHỈ ADMIN - Suspend user (ACTIVE → SUSPENDED)
    @PatchMapping("/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend active user (ADMIN only)")
    public ResponseEntity<UserResponse> suspendUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.suspendUser(userId));
    }

    // CHỈ ADMIN - Activate user (SUSPENDED/REJECTED → ACTIVE)
    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate suspended/rejected user (ADMIN only)")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.activateUser(userId));
    }
}

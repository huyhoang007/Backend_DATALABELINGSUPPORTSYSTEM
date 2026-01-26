package com.datalabeling.datalabelingsupportsystem.controller.ActivityLog;

import com.datalabeling.datalabelingsupportsystem.dto.response.ActivityLog.ActivityLogResponse;
import com.datalabeling.datalabelingsupportsystem.service.ActivityLog.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
@Tag(name = "Activity Log Management")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get all activity logs with pagination")
    public ResponseEntity<Page<ActivityLogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLogResponse> logs = activityLogService.getAllLogs(page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get activity logs by user")
    public ResponseEntity<Page<ActivityLogResponse>> getLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLogResponse> logs = activityLogService.getLogsByUser(userId, page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get activity logs by action")
    public ResponseEntity<Page<ActivityLogResponse>> getLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLogResponse> logs = activityLogService.getLogsByAction(action, page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/target/{target}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get activity logs by target")
    public ResponseEntity<Page<ActivityLogResponse>> getLogsByTarget(
            @PathVariable String target,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLogResponse> logs = activityLogService.getLogsByTarget(target, page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get activity logs by date range")
    public ResponseEntity<List<ActivityLogResponse>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<ActivityLogResponse> logs = activityLogService.getLogsByDateRange(startDate, endDate);
        return ResponseEntity.ok(logs);
    }

}

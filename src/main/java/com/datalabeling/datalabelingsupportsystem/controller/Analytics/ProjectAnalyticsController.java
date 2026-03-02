package com.datalabeling.datalabelingsupportsystem.controller.Analytics;

import com.datalabeling.datalabelingsupportsystem.dto.response.Analytics.*;
import com.datalabeling.datalabelingsupportsystem.service.Analytics.ProjectAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Project Analytics", description = "APIs for monitoring project quality and performance")
public class ProjectAnalyticsController {
    
    private final ProjectAnalyticsService analyticsService;
    
    @Operation(
            summary = "Get project progress",
            description = "Lấy thông tin tiến độ của dự án (hoàn thành, đang xử lý, chưa bắt đầu)"
    )
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/projects/{projectId}/progress")
    public ResponseEntity<ProjectProgressResponse> getProjectProgress(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId) {
        ProjectProgressResponse response = analyticsService.getProjectProgress(projectId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get project quality metrics",
            description = "Lấy chỉ số chất lượng dự án (chính xác, tuân thủ chính sách, cân bằng label)"
    )
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/projects/{projectId}/quality")
    public ResponseEntity<QualityMetricsResponse> getQualityMetrics(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId) {
        QualityMetricsResponse response = analyticsService.getProjectQualityMetrics(projectId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get team contributions",
            description = "Lấy danh sách đóng góp của các thành viên trong dự án"
    )
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/projects/{projectId}/contributions")
    public ResponseEntity<List<ContributionResponse>> getTeamContributions(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId) {
        List<ContributionResponse> response = analyticsService.getTeamContributions(projectId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get user contribution details",
            description = "Lấy chi tiết đóng góp của một thành viên cụ thể"
    )
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/projects/{projectId}/contributions/{userId}")
    public ResponseEntity<ContributionResponse> getUserContribution(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId,
            @Parameter(description = "User ID")
            @PathVariable Long userId) {
        ContributionResponse response = analyticsService.getUserContribution(projectId, userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get component quality metrics",
            description = "Lấy chất lượng các thành phần (label, policy, dataset, etc.)"
    )
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/projects/{projectId}/components")
    public ResponseEntity<List<ComponentQualityResponse>> getComponentQuality(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId) {
        List<ComponentQualityResponse> response = analyticsService.getComponentQuality(projectId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get comprehensive analytics summary",
            description = "Lấy tóm tắt phân tích toàn diện (tiến độ, chất lượng, đóng góp, cảnh báo)"
    )
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/projects/{projectId}/summary")
    public ResponseEntity<ProjectAnalyticsSummaryResponse> getProjectSummary(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId) {
        ProjectAnalyticsSummaryResponse response = analyticsService.getProjectAnalyticsSummary(projectId);
        return ResponseEntity.ok(response);
    }
}

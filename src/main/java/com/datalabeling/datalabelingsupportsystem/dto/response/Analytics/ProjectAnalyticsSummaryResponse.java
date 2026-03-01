package com.datalabeling.datalabelingsupportsystem.dto.response.Analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAnalyticsSummaryResponse {
    private Long projectId;
    private String projectName;
    private String status;
    
    // Tổng quan Progress
    private ProjectProgressResponse progress;
    
    // Tổng quan Quality
    private QualityMetricsResponse qualityMetrics;
    
    // Top Contributors
    private List<ContributionResponse> topContributors;
    
    // Team Performance
    private Integer totalTeamMembers;
    private Double teamAveragePerformanceScore;
    
    // Các cảnh báo
    private List<String> alerts; // Danh sách các cảnh báo
    
    // Thời gian cập nhật
    private LocalDateTime generatedAt;
}

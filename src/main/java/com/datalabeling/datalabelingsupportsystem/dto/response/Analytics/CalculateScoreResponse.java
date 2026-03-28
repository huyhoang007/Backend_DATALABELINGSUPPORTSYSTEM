package com.datalabeling.datalabelingsupportsystem.dto.response.Analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateScoreResponse {
    private Long projectId;
    private String projectName;
    private Double calculatedScore; // Điểm tính được
    private Double previousScore; // Điểm trước đó
    private Double scoreChange; // Thay đổi điểm
    private String scoreLevel; // EXCELLENT, GOOD, FAIR, POOR
    private String calculationMethod; // Phương pháp tính (e.g., "WEIGHTED_AVERAGE")
    private LocalDateTime calculatedAt;
}
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
public class QualityMetricsResponse {
    private Long projectId;
    private String projectName;
    
    // Chất lượng Annotation
    private Double annotationAccuracy; // Tỉ lệ annotation chính xác (%)
    private Long totalAnnotations;
    private Long acceptedAnnotations;
    private Long rejectedAnnotations;
    
    // Tuân thủ Chính sách
    private Double policyComplianceRate; // Tỉ lệ tuân thủ (%)
    private Long totalPolicyViolations;
    private Long criticalViolations; // Vi phạm nghiêm trọng
    private Long minorViolations; // Vi phạm nhẹ
    
    // Chất lượng Label
    private Integer totalLabelUsed; // Số label được sử dụng
    private Double labelDistributionBalance; // Cân bằng distribution (0-100%)
    private Long mostUsedLabelCount;
    private String mostUsedLabel;
    private Long leastUsedLabelCount;
    private String leastUsedLabel;
    
    // Review Quality
    private Double reviewAccuracy; // Tỉ lệ review chính xác (%)
    private Long totalReviewsCompleted;
    private Long improvementsFound; // Số lần tìm thấy cải tiến
    private Double improvementRate; // Tỉ lệ cải tiến (%)
    
    // Thống kê chung
    private Double overallQualityScore; // Điểm chất lượng tổng (0-100)
    private String qualityLevel; // EXCELLENT, GOOD, FAIR, POOR
    private LocalDateTime lastUpdated;
    
    // Trend
    private Double qualityTrendPercentage; // Thay đổi so với kỳ trước (%)
}

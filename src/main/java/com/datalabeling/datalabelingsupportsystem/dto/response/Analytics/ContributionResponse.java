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
public class ContributionResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String role; // ANNOTATOR, REVIEWER
    
    // Đóng góp chung
    private Long totalAssignments; // Tổng số task được gán
    private Long completedAssignments; // Số task hoàn thành
    private Double completionRate; // Tỉ lệ hoàn thành (%)
    
    // Cho Annotators
    private Long annotationsCount; // Số annotation đã thực hiện
    private Double annotationQuality; // Chất lượng annotation (0-100%)
    private Long policiesViolated; // Số lần vi phạm chính sách
    private Double policyComplianceRate; // Tỉ lệ tuân thủ chính sách (%)
    
    // Cho Reviewers
    private Long reviewsCount; // Số review đã thực hiện
    private Long approvedCount; // Số annotation đã approve
    private Long rejectedCount; // Số annotation đã reject
    private Double rejectionRate; // Tỉ lệ reject (%)
    
    // Hiệu suất
    private Long averageTimePerTask; // Trung bình thời gian per task (phút)
    private LocalDateTime lastActivityTime;
    private Integer consecutiveDaysActive;
    
    // Rating/Score
    private Double performanceScore; // Điểm hiệu suất tổng (0-100)
}

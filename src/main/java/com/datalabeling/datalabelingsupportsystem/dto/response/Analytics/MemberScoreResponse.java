package com.datalabeling.datalabelingsupportsystem.dto.response.Analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberScoreResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String role; // ANNOTATOR, REVIEWER
    
    // Điểm số chi tiết
    private Double performanceScore; // Điểm hiệu suất tổng (0-100)
    private Double completionRate; // Tỉ lệ hoàn thành công việc (0-100)
    private Double qualityScore; // Điểm chất lượng (0-100)
    private Double complianceScore; // Điểm tuân thủ chính sách (0-100)
    
    // Xếp hạng
    private Integer rank; // Xếp hạng trong dự án
    private String tier; // EXCELLENT, GOOD, AVERAGE, POOR
    
    // Thống kê
    private Long totalAssignments;
    private Long completedAssignments;
    private Long annotationsCount;
    private Long reviewsCount;
}

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
public class ProjectProgressResponse {
    private Long projectId;
    private String projectName;
    private String status;
    
    // Tiến độ chung
    private Double overallProgress; // 0-100%
    
    // Chi tiết tiến độ
    private Long totalItems; // Tổng số item cần label
    private Long labeledItems; // Số item đã labeled
    private Long reviewedItems; // Số item đã review
    private Long approvedItems; // Số item đã approved
    
    // Tỉ lệ
    private Double labelingProgress; // Tỉ lệ % labeled
    private Double reviewingProgress; // Tỉ lệ % reviewed
    private Double approvalProgress; // Tỉ lệ % approved
    
    // Thời gian
    private LocalDateTime createdAt;
    private LocalDateTime expectedCompletionDate;
    private LocalDateTime generatedAt;
    
    // Ước tính
    private Integer estimatedDaysRemaining;
}

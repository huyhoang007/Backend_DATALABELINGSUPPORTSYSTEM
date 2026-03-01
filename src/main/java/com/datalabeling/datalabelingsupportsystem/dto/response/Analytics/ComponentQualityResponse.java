package com.datalabeling.datalabelingsupportsystem.dto.response.Analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentQualityResponse {
    private String componentType; // LABEL, POLICY, DATASET, etc.
    private String componentName;
    private Long componentId;
    
    // Chất lượng thành phần
    private Long usageCount; // Số lần được sử dụng
    private Long errorCount; // Số lỗi phát hiện
    private Double qualityScore; // Điểm chất lượng (0-100)
    private String status; // HEALTHY, WARNING, CRITICAL
    
    // Chi tiết
    private Double accuracy; // Tỉ lệ chính xác
    private Long violationCount; // Số vi phạm chính sách
    
    // Recommendations
    private String recommendation; // Gợi ý cải tiến
}

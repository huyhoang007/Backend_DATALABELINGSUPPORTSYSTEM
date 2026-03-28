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
public class NormalizeDataResponse {
    private Long projectId;
    private String projectName;
    private Long totalItemsNormalized; // Tổng số items được normalize
    private Long totalLabelsNormalized; // Tổng số labels được normalize
    private Double normalizationAccuracy; // Độ chính xác của normalization (%)
    private String normalizationMethod; // Phương pháp normalize (e.g., "STANDARDIZATION")
    private LocalDateTime normalizedAt;
}
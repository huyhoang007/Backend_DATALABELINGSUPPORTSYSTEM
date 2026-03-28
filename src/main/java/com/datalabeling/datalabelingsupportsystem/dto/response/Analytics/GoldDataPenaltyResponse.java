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
public class GoldDataPenaltyResponse {
    private Long projectId;
    private String projectName;
    private Double totalPenaltyScore; // Tổng điểm phạt
    private Long totalGoldDataItems; // Tổng số gold data items
    private Long incorrectAnnotations; // Số annotation sai so với gold data
    private Double penaltyRate; // Tỉ lệ phạt (%)
    private String penaltyLevel; // NONE, LOW, MEDIUM, HIGH
    private LocalDateTime calculatedAt;
}
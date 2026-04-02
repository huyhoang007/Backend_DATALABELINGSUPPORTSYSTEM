package com.datalabeling.datalabelingsupportsystem.dto.response.Analytics;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ViolationSummaryResponse {
    private Long projectId;
    private Long total;
    private Map<String, Long> byType;
    private Map<Long, Long> byUser;
}

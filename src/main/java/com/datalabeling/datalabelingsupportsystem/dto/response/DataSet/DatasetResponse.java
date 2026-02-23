package com.datalabeling.datalabelingsupportsystem.dto.response.DataSet;

import com.datalabeling.datalabelingsupportsystem.enums.DataSet.BatchStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DatasetResponse {
    private Long datasetId;
    private String name;
    private BatchStatus status;
    private LocalDateTime createdAt;
    private Long projectId;
    private long totalItems;
}

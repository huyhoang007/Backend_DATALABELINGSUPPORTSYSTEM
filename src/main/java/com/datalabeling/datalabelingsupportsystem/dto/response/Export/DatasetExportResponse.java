package com.datalabeling.datalabelingsupportsystem.dto.response.Export;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DatasetExportResponse {
    private Long datasetId;
    private String datasetName;
    private String status;
    private LocalDateTime createdAt;
    private Long projectId;
    private String projectName;
    private String exportedStatus;  // filter được áp dụng: "ALL" | "APPROVED" | ...
    private int totalImages;
    private int totalAnnotations;
    private List<ExportDataItemDto> images;
}

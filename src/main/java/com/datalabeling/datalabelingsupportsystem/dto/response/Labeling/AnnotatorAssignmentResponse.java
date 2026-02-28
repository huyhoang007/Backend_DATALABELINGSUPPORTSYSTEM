package com.datalabeling.datalabelingsupportsystem.dto.response.Labeling;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AnnotatorAssignmentResponse {
    private Long assignmentId;
    private String projectName;
    private String datasetName;
    private String dataType;
    private String status;
    private Integer progress;
    private LocalDateTime completedAt;
    private String annotatorName;
    private String reviewerName;
}

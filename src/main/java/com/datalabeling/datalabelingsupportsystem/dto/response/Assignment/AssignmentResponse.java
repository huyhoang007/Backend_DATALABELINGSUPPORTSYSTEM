package com.datalabeling.datalabelingsupportsystem.dto.response.Assignment;

import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {

    private Long assignmentId;
    private Long projectId;
    private String projectName;
    private Long datasetId;
    private String datasetName;
    private Long annotatorId;
    private String annotatorName;
    private Long reviewerId;
    private String reviewerName;
    private AssignmentStatus status;
    private Integer progress;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}

package com.datalabeling.datalabelingsupportsystem.dto.request.Assignment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateAssignmentRequest {

    @NotNull(message = "Dataset ID is required")
    private Long datasetId;

    @NotNull(message = "Annotator ID is required")
    private Long annotatorId;

    @NotNull(message = "Reviewer ID is required")
    private Long reviewerId;
}

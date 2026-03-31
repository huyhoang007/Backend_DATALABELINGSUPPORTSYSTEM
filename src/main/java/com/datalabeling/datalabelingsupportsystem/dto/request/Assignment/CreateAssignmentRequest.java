package com.datalabeling.datalabelingsupportsystem.dto.request.Assignment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateAssignmentRequest {

    @NotNull(message = "ID bộ dữ liệu là bắt buộc")
    private Long datasetId;

    @NotNull(message = "ID người lũu ý là bắt buộc")
    private Long annotatorId;

    @NotNull(message = "ID người duyệt là bắt buộc")
    private Long reviewerId;
}

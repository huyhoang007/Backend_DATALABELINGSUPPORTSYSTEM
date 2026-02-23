package com.datalabeling.datalabelingsupportsystem.dto.request.DataSet;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDatasetRequest {
    @NotBlank(message = "Batch name is required")
    private String batchName;
}

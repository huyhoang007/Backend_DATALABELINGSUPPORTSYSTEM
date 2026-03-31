package com.datalabeling.datalabelingsupportsystem.dto.request.DataSet;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDatasetRequest {
    @NotBlank(message = "Tên batch là bắt buộc")
    private String batchName;
}

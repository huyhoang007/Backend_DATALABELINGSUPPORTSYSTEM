package com.datalabeling.datalabelingsupportsystem.dto.request.DataSet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CreateDatasetRequest {

    @NotBlank(message = "Batch name is required")
    @Schema(description = "Tên batch", example = "Human_Images_v1")
    private String batch_name;

    @NotEmpty(message = "Files must not be empty")
    @Schema(description = "Danh sách file ảnh", type = "array", format = "binary")
    private List<MultipartFile> files;
}
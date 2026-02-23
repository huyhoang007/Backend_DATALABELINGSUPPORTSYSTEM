package com.datalabeling.datalabelingsupportsystem.dto.request.DataSet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AddItemsRequest {

    @NotEmpty(message = "Files must not be empty")
    @Schema(description = "Danh sách file ảnh bổ sung", type = "array", format = "binary")
    private List<MultipartFile> files;
}

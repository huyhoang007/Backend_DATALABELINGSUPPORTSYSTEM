package com.datalabeling.datalabelingsupportsystem.dto.request.Project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Tên dự án là bắt buộc")
    private String name;

    @NotBlank(message = "Loại dữ liệu là bắt buộc")
    @Pattern(regexp = "IMAGE", message = "Loại dữ liệu phải là IMAGE")
    private String dataType;

    private String description;

    private String guidelineContent;

    private String guidelineVersion;

    private String guidelineFileUrl;
}

package com.datalabeling.datalabelingsupportsystem.dto.request.Project;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProjectRequest {

    private String name;

    @Pattern(regexp = "IMAGE", message = "Loại dữ liệu phải là IMAGE")
    private String dataType;

    private String description;

    private String guidelineContent;

    private String guidelineVersion;

    private String guidelineFileUrl;

    @Pattern(
            regexp = "DRAFT|IN_PROGRESS|PAUSED",
            message = "Trạng thái phải là DRAFT, IN_PROGRESS, PAUSED")
    private String status;
}

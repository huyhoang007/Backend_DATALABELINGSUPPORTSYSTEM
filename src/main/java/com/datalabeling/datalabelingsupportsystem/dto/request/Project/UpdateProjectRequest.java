package com.datalabeling.datalabelingsupportsystem.dto.request.Project;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProjectRequest {

    private String name;

    @Pattern(regexp = "IMAGE|VIDEO|TEXT|AUDIO", message = "Data type must be IMAGE, VIDEO, TEXT, or AUDIO")
    private String dataType;

    private String description;

    private String guidelineContent;

    private String guidelineVersion;

    private String guidelineFileUrl;

    @Pattern(
            regexp = "DRAFT|IN_PROGRESS|PAUSED|COMPLETED",
            message = "Status must be DRAFT, IN_PROGRESS, PAUSED, or COMPLETED")
    private String status;
}

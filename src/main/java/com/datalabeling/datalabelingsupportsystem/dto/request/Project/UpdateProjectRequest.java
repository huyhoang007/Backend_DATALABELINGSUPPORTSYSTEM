package com.datalabeling.datalabelingsupportsystem.dto.request.Project;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProjectRequest {

    private String name;

    @Pattern(regexp = "IMAGE|VIDEO|TEXT|AUDIO", message = "Data type must be IMAGE, VIDEO, TEXT, or AUDIO")
    private String dataType;

    private String description;

    @Pattern(regexp = "ACTIVE|INACTIVE|COMPLETED", message = "Status must be ACTIVE, INACTIVE, or COMPLETED")
    private String status;
}

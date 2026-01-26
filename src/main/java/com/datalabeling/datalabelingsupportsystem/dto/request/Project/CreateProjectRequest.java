package com.datalabeling.datalabelingsupportsystem.dto.request.Project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Data type is required")
    @Pattern(regexp = "IMAGE|VIDEO|TEXT|AUDIO", message = "Data type must be IMAGE, VIDEO, TEXT, or AUDIO")
    private String dataType;

    private String description;
}

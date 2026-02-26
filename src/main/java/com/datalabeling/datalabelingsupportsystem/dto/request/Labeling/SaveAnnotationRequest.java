package com.datalabeling.datalabelingsupportsystem.dto.request.Labeling;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveAnnotationRequest {

    @NotNull(message = "labelId is required")
    private Long labelId;

    @NotNull(message = "itemId is required")
    private Long itemId;

    @NotBlank(message = "geometry is required")
    private String geometry; // JSON string

}

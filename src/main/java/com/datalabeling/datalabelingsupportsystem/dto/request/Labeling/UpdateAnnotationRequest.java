package com.datalabeling.datalabelingsupportsystem.dto.request.Labeling;

import lombok.Data;

@Data
public class UpdateAnnotationRequest {
    private Long labelId;
    private String geometry;
}

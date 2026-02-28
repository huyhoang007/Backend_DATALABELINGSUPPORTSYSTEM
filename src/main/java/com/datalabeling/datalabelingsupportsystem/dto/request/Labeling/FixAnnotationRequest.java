package com.datalabeling.datalabelingsupportsystem.dto.request.Labeling;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FixAnnotationRequest {

    // Label mới (null = giữ nguyên label cũ)
    private Long labelId;

    // Geometry mới (null = giữ nguyên geometry cũ)
    private JsonNode geometry;
}

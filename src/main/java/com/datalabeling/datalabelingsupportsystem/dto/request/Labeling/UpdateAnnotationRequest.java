package com.datalabeling.datalabelingsupportsystem.dto.request.Labeling;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class UpdateAnnotationRequest {
    private Long labelId;
    // Nhận cả JSON object lẫn null từ frontend
    private JsonNode geometry;
}

package com.datalabeling.datalabelingsupportsystem.dto.request.Labeling;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchSaveAnnotationRequest {

    @NotNull(message = "itemId is required")
    private Long itemId;

    @NotEmpty(message = "annotations must not be empty")
    @Valid
    private List<AnnotationItem> annotations;

    @Data
    public static class AnnotationItem {

        @NotNull(message = "labelId is required")
        private Long labelId;

        // geometry có thể null (classification task không cần vùng)
        private JsonNode geometry;
    }
}

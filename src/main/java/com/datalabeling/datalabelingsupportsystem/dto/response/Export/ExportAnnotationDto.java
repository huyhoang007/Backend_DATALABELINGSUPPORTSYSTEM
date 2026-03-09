package com.datalabeling.datalabelingsupportsystem.dto.response.Export;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportAnnotationDto {
    private Long reviewingId;
    private Long labelId;
    private String labelName;
    private String labelType;
    private String colorCode;
    @JsonRawValue
    private String geometry;       // raw JSON string: [{"x":..,"y":..}, ...]
    private String status;         // APPROVED / PENDING / REJECTED / IMPROVED
    private Boolean isImproved;
    private String annotatorName;
    private String reviewerName;
}

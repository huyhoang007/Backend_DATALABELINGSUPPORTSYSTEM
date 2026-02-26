package com.datalabeling.datalabelingsupportsystem.dto.response.Labeling;

import com.datalabeling.datalabelingsupportsystem.enums.Reviewing.ReviewingStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnotationResponse {
    private Long reviewingId;
    private Long itemId;
    private Long labelId;
    private String labelName;
    private String colorCode;
    private String labelType;
    private String geometry;
    private ReviewingStatus status;
    private Boolean isImproved;
}

package com.datalabeling.datalabelingsupportsystem.dto.response.DataItem;

import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotationResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataItemResponse {
    private Long itemId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Integer width;
    private Integer height;
    private Boolean isActive;
    private List<AnnotationResponse> annotations;
}

package com.datalabeling.datalabelingsupportsystem.dto.response.DataItem;

import lombok.Builder;
import lombok.Data;

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
}

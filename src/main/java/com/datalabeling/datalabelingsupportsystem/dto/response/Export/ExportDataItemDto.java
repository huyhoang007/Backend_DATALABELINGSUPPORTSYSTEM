package com.datalabeling.datalabelingsupportsystem.dto.response.Export;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExportDataItemDto {
    private Long itemId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Integer width;
    private Integer height;
    private List<ExportAnnotationDto> annotations;
}

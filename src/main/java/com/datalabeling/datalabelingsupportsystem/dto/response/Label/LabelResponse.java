package com.datalabeling.datalabelingsupportsystem.dto.response.Label;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponse {
    private Long labelId;
    private String labelName;
    private String colorCode;
    private String labelType;
    private String description;
    private String shortcutKey;
    private Boolean isActive;
}

package com.datalabeling.datalabelingsupportsystem.dto.request.Label;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class CreateLabelRuleRequest {
    @NotBlank
    private String name;

    private String ruleContent;

    private Set<Long> labelIds;
}

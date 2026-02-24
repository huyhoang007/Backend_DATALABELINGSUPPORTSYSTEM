package com.datalabeling.datalabelingsupportsystem.dto.request.Label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class CreateLabelRuleRequest {
    @NotBlank
    private String name;

    private String ruleContent;

    @NotEmpty(message = "At least one label must be provided")
    private Set<Long> labelIds;
}

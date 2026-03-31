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

    @NotEmpty(message = "Phải cung cấp ít nhất một nhãn")
    private Set<Long> labelIds;
}

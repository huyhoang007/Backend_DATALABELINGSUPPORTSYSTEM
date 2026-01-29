package com.datalabeling.datalabelingsupportsystem.dto.response.Label;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelRuleResponse {
    private Long ruleId;
    private String name;
    private String ruleContent;
    private Set<LabelResponse> labels;
}

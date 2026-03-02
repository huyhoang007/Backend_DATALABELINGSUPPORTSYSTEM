package com.datalabeling.datalabelingsupportsystem.dto.request.Project;

import lombok.Data;

import java.util.List;

@Data
public class SetProjectLabelRulesRequest {
    private List<Long> ruleIds;
}

package com.datalabeling.datalabelingsupportsystem.dto.response.Label;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LabelGroupResponse {
    private Long ruleId;
    private String ruleName;            // "Vehicle", "Person"
    private List<LabelResponse> labels; // [Truck, Car, Container...]
}

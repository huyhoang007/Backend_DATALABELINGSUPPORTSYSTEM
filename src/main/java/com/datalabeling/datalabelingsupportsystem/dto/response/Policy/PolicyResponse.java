package com.datalabeling.datalabelingsupportsystem.dto.response.Policy;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PolicyResponse {

    private Long policyId;
    private String errorName;
    private String description;
}

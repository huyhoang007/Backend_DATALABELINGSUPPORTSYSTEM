package com.datalabeling.datalabelingsupportsystem.dto.request.Policy;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePolicyRequest {

    @NotBlank
    private String errorName;

    private String description;
}

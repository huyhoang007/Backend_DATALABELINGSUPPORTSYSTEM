package com.datalabeling.datalabelingsupportsystem.dto.request.Policy;

import com.datalabeling.datalabelingsupportsystem.enums.Policies.ErrorLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePolicyRequest {
    private String errorName;
    private String description;
    private ErrorLevel errorLevel;
}

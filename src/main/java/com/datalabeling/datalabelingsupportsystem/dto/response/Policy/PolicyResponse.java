package com.datalabeling.datalabelingsupportsystem.dto.response.Policy;

import com.datalabeling.datalabelingsupportsystem.enums.Policies.ErrorLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
    private Long policyId;
    private String errorName;
    private String description;
    private ErrorLevel errorLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

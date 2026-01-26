package com.datalabeling.datalabelingsupportsystem.dto.request.Policy;

import lombok.Data;

@Data
public class CreatePolicyRequest {
    private String name;
    private String description;
    private String content;
    private String status;
}

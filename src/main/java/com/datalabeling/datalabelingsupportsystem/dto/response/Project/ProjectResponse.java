package com.datalabeling.datalabelingsupportsystem.dto.response.Project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long projectId;
    private String name;
    private String dataType;
    private String status;
    private String description;
    private String managerName;
    private Long managerId;
    private LocalDateTime createdAt;
}

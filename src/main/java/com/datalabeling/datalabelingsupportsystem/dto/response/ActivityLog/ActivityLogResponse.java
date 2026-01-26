package com.datalabeling.datalabelingsupportsystem.dto.response.ActivityLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long activityId;
    private Long userId;
    private String username;
    private String action;
    private String target;
    private String details;
    private LocalDateTime createdAt;
}

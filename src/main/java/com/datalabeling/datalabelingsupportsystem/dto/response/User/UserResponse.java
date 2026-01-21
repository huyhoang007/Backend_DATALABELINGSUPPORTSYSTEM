package com.datalabeling.datalabelingsupportsystem.dto.response.User;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String status;
    private String role;
    private LocalDateTime createdAt;
}

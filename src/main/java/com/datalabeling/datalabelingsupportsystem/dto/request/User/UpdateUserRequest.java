package com.datalabeling.datalabelingsupportsystem.dto.request.User;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String email;
    private String fullName;
    private String status;  // PENDING, ACTIVE, INACTIVE
    private Long roleId;    // New field to change role
}

package com.datalabeling.datalabelingsupportsystem.dto.request.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String roleName; // ADMIN, MANAGER, ...
}

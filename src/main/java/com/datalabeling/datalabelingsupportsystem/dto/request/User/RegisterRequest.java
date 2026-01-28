package com.datalabeling.datalabelingsupportsystem.dto.request.User;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
}


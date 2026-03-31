package com.datalabeling.datalabelingsupportsystem.dto.request.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "Tên đăng nhập là bắt buộc")
    private String username;
    
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;
    
    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;
    
    @NotBlank(message = "Họtên là bắt buộc")
    private String fullName;
    
    @NotNull(message = "ID vai trò là bắt buộc")
    private Long roleId; // 1=ADMIN, 2=MANAGER, 3=ANNOTATOR, 4=REVIEWER
}

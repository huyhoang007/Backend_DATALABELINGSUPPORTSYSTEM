package com.datalabeling.datalabelingsupportsystem.controller.User;

import com.datalabeling.datalabelingsupportsystem.dto.request.User.CreateUserRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.UserResponse;
import com.datalabeling.datalabelingsupportsystem.service.User.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }
}

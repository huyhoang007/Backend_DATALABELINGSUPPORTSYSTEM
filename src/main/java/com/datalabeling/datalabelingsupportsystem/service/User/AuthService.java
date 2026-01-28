package com.datalabeling.datalabelingsupportsystem.service.User;

import com.datalabeling.datalabelingsupportsystem.config.JWT.JwtService;
import com.datalabeling.datalabelingsupportsystem.dto.request.User.LoginRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.User.RegisterRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.AuthResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.UserResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.Role;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Users.RoleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername((request.getUsername()))) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role annotatorRole = roleRepository.findByRoleName("ANNOTATOR")
                .orElseThrow(() -> new RuntimeException("Default ANNOTATOR role not found"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(annotatorRole)
                .status("PENDING")
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .status(savedUser.getStatus())
                .roleName(savedUser.getRole().getRoleName())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Kiểm tra status của user
        String status = user.getStatus();
        if (status == null || !"ACTIVE".equals(status)) {
            String message;
            if (status == null) {
                message = "Your account status is undefined. Please contact administrator.";
            } else if ("PENDING".equals(status)) {
                message = "Your account is pending approval. Please wait for admin to activate your account.";
            } else if ("BANNED".equals(status)) {
                message = "Your account has been banned. Please contact administrator.";
            } else if ("REJECTED".equals(status)) {
                message = "Your account has been rejected. Please contact administrator.";
            } else if ("SUSPENDED".equals(status)) {
                message = "Your account has been suspended. Please contact administrator.";
            } else {
                message = "Your account is not active. Please contact administrator.";
            }
            throw new RuntimeException(message);
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                user.getUsername(),
                user.getRole().getRoleName()
        );
    }
}
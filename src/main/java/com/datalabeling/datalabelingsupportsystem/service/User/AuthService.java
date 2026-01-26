package com.datalabeling.datalabelingsupportsystem.service.User;

import com.datalabeling.datalabelingsupportsystem.config.JWT.JwtService;
import com.datalabeling.datalabelingsupportsystem.dto.request.User.LoginRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.User.RegisterRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.AuthResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.Role;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Users.RoleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {

        if (userRepository.existsByUsername((request.getUsername()))) {
            throw new RuntimeException("Username already exists");
        }

        Role role = roleRepository.findByRoleName("ANNOTATOR")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
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
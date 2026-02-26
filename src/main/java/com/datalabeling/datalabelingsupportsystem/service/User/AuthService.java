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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.allow-register-roles:false}")
    private boolean allowRegisterRoles;

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER", "REVIEWER", "ANNOTATOR");

    // Custom Exceptions
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Input validation
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }

        // Check duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username already exists - {}", request.getUsername());
            throw new DuplicateResourceException("USERNAME_ALREADY_EXISTS");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists - {}", request.getEmail());
            throw new DuplicateResourceException("EMAIL_ALREADY_EXISTS");
        }

        log.info("Registering new user: {}, requested role: {}", request.getUsername(), request.getRole());

        // Role Logic
        String targetRoleName = "ANNOTATOR"; // Default
        if (allowRegisterRoles && request.getRole() != null) {
            String requestedRole = request.getRole().toUpperCase();
            if (ALLOWED_ROLES.contains(requestedRole)) {
                targetRoleName = requestedRole;
            } else {
                log.warn("Requested invalid role: {}. Defaulting to ANNOTATOR.", requestedRole);
                // Optionally throw validation exception here if strict
            }
        }

        String finalTargetRoleName = targetRoleName;
        Role role = roleRepository.findByRoleName(finalTargetRoleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + finalTargetRoleName));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .status("PENDING")
                .build();

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {} with role {}", user.getUsername(), role.getRoleName());
        
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
        // Validation - accept username OR email
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username or email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }

        String identifier = request.getUsername().trim();
        log.info("Login attempt for identifier: {}", identifier);

        // Try username first, then email (case-insensitive for email)
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found - {}", identifier);
                    return new AuthenticationException("Invalid credentials");
                });

        // Password verification using BCrypt encoder (consistent with register)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for user - {}", user.getUsername());
            throw new AuthenticationException("Invalid credentials");
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
        log.info("Login successful for user: {}", user.getUsername());

        return new AuthResponse(
                token,
                "Bearer",
                user.getUsername(),
                user.getRole().getRoleName());
    }
}
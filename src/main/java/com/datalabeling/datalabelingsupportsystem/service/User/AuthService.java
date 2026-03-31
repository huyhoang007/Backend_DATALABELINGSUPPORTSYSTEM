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
            throw new ValidationException("Tên đăng nhập được yêu cầu");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email được yêu cầu");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ValidationException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        // Check duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Đăng ký thất bại: tên đăng nhập đã tồn tại - {}", request.getUsername());
            throw new DuplicateResourceException("USERNAME_ALREADY_EXISTS");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Đăng ký thất bại: email đã tồn tại - {}", request.getEmail());
            throw new DuplicateResourceException("EMAIL_ALREADY_EXISTS");
        }

        log.info("Đăng ký người dùng mới: {}, vai trò được yêu cầu: {}", request.getUsername(), request.getRole());

        // Role Logic
        String targetRoleName = "ANNOTATOR"; // Default
        if (allowRegisterRoles && request.getRole() != null) {
            String requestedRole = request.getRole().toUpperCase();
            if (ALLOWED_ROLES.contains(requestedRole)) {
                targetRoleName = requestedRole;
            } else {
                log.warn("Yêu cầu vai trò không hợp lệ: {}. Mặc định là ANNOTATOR.", requestedRole);
                // Optionally throw validation exception here if strict
            }
        }

        String finalTargetRoleName = targetRoleName;
        Role role = roleRepository.findByRoleName(finalTargetRoleName)
                .orElseThrow(() -> new RuntimeException("Vai trò không được tìm thấy: " + finalTargetRoleName));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .status("PENDING")
                .build();

        User savedUser = userRepository.save(user);

        log.info("Người dùng đăng ký thành công: {} với vai trò {}", user.getUsername(), role.getRoleName());
        
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
            throw new ValidationException("Tên đăng nhập hoặc email được yêu cầu");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new ValidationException("Mật khẩu được yêu cầu");
        }

        String identifier = request.getUsername().trim();
        log.info("Đăng nhập cho định danh: {}", identifier);

        // Try username first, then email (case-insensitive for email)
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: người dùng không được tìm thấy - {}", identifier);
                    return new AuthenticationException("Thông tin đăng nhập không hợp lệ");
                });

        // Password verification using BCrypt encoder (consistent with register)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Đăng nhập thất bại: mật khẩu không hợp lệ cho người dùng - {}", user.getUsername());
            throw new AuthenticationException("Thông tin đăng nhập không hợp lệ");
        }

        // Kiểm tra status của user
        String status = user.getStatus();
        if (status == null || !"ACTIVE".equals(status)) {
            String message;
            if (status == null) {
                message = "Trạng thái tài khoản của bạn chưa xác định. Vui lòng liên hệ quản trị viên.";
            } else if ("PENDING".equals(status)) {
                message = "Tài khoản của bạn đang chờ phê duyệt. Vui lòng chờ quản trị viên kích hoạt tài khoản của bạn.";
            } else if ("BANNED".equals(status)) {
                message = "Tài khoản của bạn đã bị cấm. Vui lòng liên hệ quản trị viên.";
            } else if ("REJECTED".equals(status)) {
                message = "Tài khoản của bạn đã bị từ chối. Vui lòng liên hệ quản trị viên.";
            } else if ("SUSPENDED".equals(status)) {
                message = "Tài khoản của bạn đã bị tạm dừng. Vui lòng liên hệ quản trị viên.";
            } else {
                message = "Tài khoản của bạn không hoạt động. Vui lòng liên hệ quản trị viên.";
            }
            throw new RuntimeException(message);
        }

        String token = jwtService.generateToken(user);
        log.info("Đăng nhập thành công cho người dùng: {}", user.getUsername());

        return new AuthResponse(
                token,
                "Bearer",
                user.getUsername(),
                user.getRole().getRoleName());
    }
}
package com.datalabeling.datalabelingsupportsystem.service.Implementation;

import com.datalabeling.datalabelingsupportsystem.dto.request.User.CreateUserRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.UserResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.Role;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Users.RoleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import com.datalabeling.datalabelingsupportsystem.service.User.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .status("ACTIVE")
                .role(role)
                .build();

        userRepository.save(user);

        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .role(user.getRole().getRoleName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

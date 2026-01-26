package com.datalabeling.datalabelingsupportsystem.service.User;

import com.datalabeling.datalabelingsupportsystem.dto.request.User.UpdateUserRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.User.UserResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Users.RoleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

      //Lấy thông tin user hiện tại - TẤT CẢ ROLE đều dùng được

    public UserResponse getCurrentUser() {
        User user = getCurrentAuthenticatedUser();
        return mapToResponse(user);
    }

     // Update thông tin user hiện tại - TẤT CẢ ROLE đều dùng được
    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        
        // User chỉ có thể update: fullName, email (KHÔNG đổi role, status)
        if (request.getFullName() != null) {
            currentUser.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null) {
            // Kiểm tra email đã tồn tại chưa (trừ email của chính mình)
            if (userRepository.existsByEmail(request.getEmail()) 
                && !currentUser.getEmail().equals(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            currentUser.setEmail(request.getEmail());
        }
        
        User updatedUser = userRepository.save(currentUser);
        return mapToResponse(updatedUser);
    }


     //Lấy tất cả users - CHỈ ADMIN (thêm phân trang)

    public Page<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable).map(this::mapToResponse);
    }


     //Lấy user theo ID - CHỈ ADMIN

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }


     //Update user KHÁC (dùng cho ADMIN hoặc tự update)

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = "ADMIN".equals(currentUser.getRole().getRoleName());
        boolean isSelf = currentUser.getUserId().equals(userId);

        // Chỉ cho phép: ADMIN update bất kỳ ai, hoặc user tự update chính mình
        if (!isAdmin && !isSelf) {
            throw new RuntimeException("Unauthorized: You can only update your own profile");
        }

        // Update thông tin cơ bản
        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail()) 
                && !targetUser.getEmail().equals(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            targetUser.setEmail(request.getEmail());
        }
        
        if (request.getFullName() != null) {
            targetUser.setFullName(request.getFullName());
        }
        
        // CHỈ ADMIN mới đổi được status và role
        if (isAdmin) {
            if (request.getStatus() != null) {
                targetUser.setStatus(request.getStatus());
            }
            
            if (request.getRoleId() != null) {
                com.datalabeling.datalabelingsupportsystem.pojo.Role role = 
                    roleRepository.findById(request.getRoleId())
                        .orElseThrow(() -> new RuntimeException("Role not found"));
                targetUser.setRole(role);
            }
        }

        User savedUser = userRepository.save(targetUser);
        return mapToResponse(savedUser);
    }


     //Xóa user - CHỈ ADMIN

    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = getCurrentAuthenticatedUser();
        
        // Không cho phép xóa chính mình
        if (currentUser.getUserId().equals(userId)) {
            throw new RuntimeException("Cannot delete yourself");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        userRepository.delete(user);
    }


      //Lấy user đang đăng nhập

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

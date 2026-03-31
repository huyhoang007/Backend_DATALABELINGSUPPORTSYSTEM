package com.datalabeling.datalabelingsupportsystem.service.ActivityLog;

import com.datalabeling.datalabelingsupportsystem.dto.response.ActivityLog.ActivityLogResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.ActivityLog;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.ActivityLog.ActivityLogRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {
    
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public void logActivity(String action, String target, String details) {
        try {
            log.info("========== BẮT ĐẦU logActivity ==========");
            log.info("Hành động: {}, Mục tiêu: {}, Chi tiết: {}", action, target, details);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                log.error("❄ Authentication là NULL");
                return;
            }
            
            log.info("Xác thực: {}", authentication);
            log.info("Người dùng: {}", authentication.getPrincipal());
            log.info("Được xác thực: {}", authentication.isAuthenticated());
            
            if (authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
                String username = authentication.getName();
                log.info("✋ Username từ auth: {}", username);
                
                User user = userRepository.findByUsername(username).orElse(null);
                
                if (user == null) {
                    log.error("❄ Người dùng '{}' KHÔNG ĐượC TÌM THẤY trong cơ sở dữ liệu", username);
                    return;
                }
                
                log.info("✋ Người dùng tìm thấy: ID={}, Username={}", user.getUserId(), user.getUsername());
                
                ActivityLog activityLog = ActivityLog.builder()
                        .user(user)
                        .action(action)
                        .target(target)
                        .details(details)
                        .build();
                
                log.info("Đang lưu nhật ký hoạt động...");
                ActivityLog saved = activityLogRepository.save(activityLog);
                log.info("✋✋✋ Nhật ký hoạt động được lưu thành công! ID: {}", saved.getActivityId());
                
            } else {
                log.warn("⚠ Người dùng chưa được xác thực hoặc là người dùng ẩn danh");
            }
            
            log.info("========== KẾT THÚC logActivity ==========");
            
        } catch (Exception e) {
            log.error("❄❄❄ EXCEPTION trong logActivity: {}", e.getMessage(), e);
        }
    }
    
    public Page<ActivityLogResponse> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activityLogRepository.findByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }
    
    public Page<ActivityLogResponse> getLogsByUser(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không được tìm thấy"));
        Pageable pageable = PageRequest.of(page, size);
        return activityLogRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }
    
    public Page<ActivityLogResponse> getLogsByAction(String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activityLogRepository.findByActionOrderByCreatedAtDesc(action, pageable)
                .map(this::mapToResponse);
    }
    
    public Page<ActivityLogResponse> getLogsByTarget(String target, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activityLogRepository.findByTargetOrderByCreatedAtDesc(target, pageable)
                .map(this::mapToResponse);
    }
    
    public List<ActivityLogResponse> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return activityLogRepository.findByCreatedAtBetween(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private ActivityLogResponse mapToResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .activityId(log.getActivityId())
                .userId(log.getUser().getUserId())
                .username(log.getUser().getUsername())
                .action(log.getAction())
                .target(log.getTarget())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

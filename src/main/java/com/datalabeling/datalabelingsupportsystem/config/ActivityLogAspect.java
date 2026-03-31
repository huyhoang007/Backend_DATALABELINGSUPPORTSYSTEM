package com.datalabeling.datalabelingsupportsystem.config;

import com.datalabeling.datalabelingsupportsystem.dto.request.Label.CreateLabelRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Project.CreateProjectRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Project.ProjectResponse;
import com.datalabeling.datalabelingsupportsystem.service.ActivityLog.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;

    @AfterReturning(pointcut = "execution(* com.datalabeling.datalabelingsupportsystem.service..*.*create*(..)) " +
            "&& !execution(* com.datalabeling.datalabelingsupportsystem.service.ActivityLog..*(..))", returning = "result")
    public void logCreate(JoinPoint joinPoint, Object result) {
        try {
            String target = extractTarget(joinPoint);
            String details = buildCreateDetails(target, result, joinPoint.getArgs());
            activityLogService.logActivity("CREATE", target, details);
        } catch (Exception e) {
            log.error("Không thể ghi nhật ký hoạt động TạO", e);
        }
    }
    

    @AfterReturning(pointcut = "execution(* com.datalabeling.datalabelingsupportsystem.service..*.*update*(..)) " +
            "&& !execution(* com.datalabeling.datalabelingsupportsystem.service.ActivityLog..*(..))", returning = "result")
    public void logUpdate(JoinPoint joinPoint, Object result) {
        try {
            String target = extractTarget(joinPoint);
            String details = buildUpdateDetails(target, result, joinPoint.getArgs());
            activityLogService.logActivity("UPDATE", target, details);
        } catch (Exception e) {
            log.error("Không thể ghi nhật ký hoạt động CậP NHậT", e);
        }
    }

    @AfterReturning(pointcut = "execution(* com.datalabeling.datalabelingsupportsystem.service..*.*delete*(..)) " +
            "&& !execution(* com.datalabeling.datalabelingsupportsystem.service.ActivityLog..*(..))")
    public void logDelete(JoinPoint joinPoint) {
        try {
            String target = extractTarget(joinPoint);
            String details = buildDeleteDetails(target, joinPoint.getArgs());
            activityLogService.logActivity("DELETE", target, details);
        } catch (Exception e) {
            log.error("Không thể ghi nhật ký hoạt động XÓA", e);
        }
    }

    private String extractTarget(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("Service", "").toUpperCase();
    }

    private String buildCreateDetails(String target, Object result, Object[] args) {
        StringBuilder details = new StringBuilder();

        try {
            if (result instanceof ProjectResponse) {
                ProjectResponse project = (ProjectResponse) result;
                details.append("Tạo dự án '").append(project.getName())
                        .append("' (ID: ").append(project.getProjectId())
                        .append(") có loại dữ liệu: ").append(project.getDataType())
                        .append(", trạng thái: ").append(project.getStatus());

                if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                    details.append(", mô tả: '")
                            .append(truncate(project.getDescription(), 50))
                            .append("'");
                }

            } else if (result instanceof LabelResponse) {
                LabelResponse label = (LabelResponse) result;
                details.append("Tạo nhãn '").append(label.getLabelName())
                        .append("' (ID: ").append(label.getLabelId())
                        .append(") có loại: ").append(label.getLabelType())
                        .append(", màu: ").append(label.getColorCode());

                if (label.getDescription() != null && !label.getDescription().isEmpty()) {
                    details.append(", mô tả: '")
                            .append(truncate(label.getDescription(), 50))
                            .append("'");
                }

            } else {
                // Fallback: Thử lấy thông tin từ request
                details.append("Tạo ").append(target.toLowerCase());

                if (args.length > 0) {
                    if (args[0] instanceof CreateProjectRequest) {
                        CreateProjectRequest req = (CreateProjectRequest) args[0];
                        details.append(" '").append(req.getName()).append("'");
                    } else if (args[0] instanceof CreateLabelRequest) {
                        CreateLabelRequest req = (CreateLabelRequest) args[0];
                        details.append(" '").append(req.getLabelName()).append("'");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xây dựng thông báo chi tiết cho hoạt động TạO", e);
            details.append("Tạo ").append(target.toLowerCase());
        }

        return details.toString();
    }

    private String buildUpdateDetails(String target, Object result, Object[] args) {
        StringBuilder details = new StringBuilder();

        try {
            if (result instanceof ProjectResponse) {
                ProjectResponse project = (ProjectResponse) result;
                details.append("Cập nhật dự án '").append(project.getName())
                        .append("' (ID: ").append(project.getProjectId())
                        .append(") - Trạng thái hiện tại: ").append(project.getStatus())
                        .append(", loại dữ liệu: ").append(project.getDataType());

            } else if (result instanceof LabelResponse) {
                LabelResponse label = (LabelResponse) result;
                details.append("Cập nhật nhãn '").append(label.getLabelName())
                        .append("' (ID: ").append(label.getLabelId())
                        .append(") - Loại: ").append(label.getLabelType())
                        .append(", màu: ").append(label.getColorCode());

            } else {
                // Fallback
                details.append("Cập nhật ").append(target.toLowerCase());
                if (args.length > 0 && args[0] instanceof Long) {
                    details.append(" với ID: ").append(args[0]);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xây dựng thông báo chi tiết cho hoạt động CậP NHậT", e);
            details.append("Cập nhật ").append(target.toLowerCase());
        }

        return details.toString();
    }

    private String buildDeleteDetails(String target, Object[] args) {
        StringBuilder details = new StringBuilder("Đã xoá ").append(target.toLowerCase());

        if (args.length > 0 && args[0] instanceof Long) {
            details.append(" có ID: ").append(args[0]);
        }

        return details.toString();
    }

    /**
     * Helper method để cắt chuỗi dài
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}

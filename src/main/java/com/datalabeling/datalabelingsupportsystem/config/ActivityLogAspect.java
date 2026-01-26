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
            log.error("Failed to log CREATE activity", e);
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
            log.error("Failed to log UPDATE activity", e);
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
            log.error("Failed to log DELETE activity", e);
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
                details.append("Created project '").append(project.getName())
                        .append("' (ID: ").append(project.getProjectId())
                        .append(") with data type: ").append(project.getDataType())
                        .append(", status: ").append(project.getStatus());

                if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                    details.append(", description: '")
                            .append(truncate(project.getDescription(), 50))
                            .append("'");
                }

            } else if (result instanceof LabelResponse) {
                LabelResponse label = (LabelResponse) result;
                details.append("Created label '").append(label.getLabelName())
                        .append("' (ID: ").append(label.getLabelId())
                        .append(") with type: ").append(label.getLabelType())
                        .append(", color: ").append(label.getColorCode());

                if (label.getDescription() != null && !label.getDescription().isEmpty()) {
                    details.append(", description: '")
                            .append(truncate(label.getDescription(), 50))
                            .append("'");
                }

            } else {
                // Fallback: Thử lấy thông tin từ request
                details.append("Created ").append(target.toLowerCase());

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
            log.warn("Could not build detailed message for CREATE", e);
            details.append("Created ").append(target.toLowerCase());
        }

        return details.toString();
    }

    private String buildUpdateDetails(String target, Object result, Object[] args) {
        StringBuilder details = new StringBuilder();

        try {
            if (result instanceof ProjectResponse) {
                ProjectResponse project = (ProjectResponse) result;
                details.append("Updated project '").append(project.getName())
                        .append("' (ID: ").append(project.getProjectId())
                        .append(") - Current status: ").append(project.getStatus())
                        .append(", data type: ").append(project.getDataType());

            } else if (result instanceof LabelResponse) {
                LabelResponse label = (LabelResponse) result;
                details.append("Updated label '").append(label.getLabelName())
                        .append("' (ID: ").append(label.getLabelId())
                        .append(") - Type: ").append(label.getLabelType())
                        .append(", color: ").append(label.getColorCode());

            } else {
                // Fallback
                details.append("Updated ").append(target.toLowerCase());
                if (args.length > 0 && args[0] instanceof Long) {
                    details.append(" with ID: ").append(args[0]);
                }
            }
        } catch (Exception e) {
            log.warn("Could not build detailed message for UPDATE", e);
            details.append("Updated ").append(target.toLowerCase());
        }

        return details.toString();
    }

    private String buildDeleteDetails(String target, Object[] args) {
        StringBuilder details = new StringBuilder("Deleted ").append(target.toLowerCase());

        if (args.length > 0 && args[0] instanceof Long) {
            details.append(" with ID: ").append(args[0]);
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

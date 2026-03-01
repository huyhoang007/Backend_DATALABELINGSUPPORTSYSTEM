package com.datalabeling.datalabelingsupportsystem.service.Analytics;

import com.datalabeling.datalabelingsupportsystem.dto.response.Analytics.*;
import com.datalabeling.datalabelingsupportsystem.pojo.*;
import com.datalabeling.datalabelingsupportsystem.repository.Analytics.AnalyticsRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.AssignmentRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Labeling.ReviewingRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAnalyticsService {
    
    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final ReviewingRepository reviewingRepository;
    private final AnalyticsRepository analyticsRepository;
    private final DatasetRepository datasetRepository;
    private final LabelRepository labelRepository;
    private final UserRepository userRepository;
    
    /**
     * Lấy tiến độ dự án
     */
    public ProjectProgressResponse getProjectProgress(Long projectId) {
        Project project = getProjectAndValidateAccess(projectId);
        
        long totalItems = analyticsRepository.countTotalItemsByProject(projectId);
        long labeledItems = analyticsRepository.countLabeledItemsByProject(projectId);
        long approvedItems = analyticsRepository.countApprovedItemsByProject(projectId);
        
        double labelingProgress = totalItems > 0 ? (double) labeledItems / totalItems * 100 : 0;
        double approvalProgress = totalItems > 0 ? (double) approvedItems / totalItems * 100 : 0;
        double overallProgress = approvalProgress; // Tính progress chính là tỉ lệ approved
        
        return ProjectProgressResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .status(project.getStatus())
                .overallProgress(Math.min(overallProgress, 100.0))
                .totalItems(totalItems)
                .labeledItems(labeledItems)
                .approvedItems(approvedItems)
                .reviewedItems(labeledItems) // Reviewed = Labeled
                .labelingProgress(Math.min(labelingProgress, 100.0))
                .approvalProgress(Math.min(approvalProgress, 100.0))
                .reviewingProgress(Math.min(labelingProgress, 100.0))
                .createdAt(project.getCreatedAt())
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Lấy chỉ số chất lượng dự án
     */
    public QualityMetricsResponse getProjectQualityMetrics(Long projectId) {
        Project project = getProjectAndValidateAccess(projectId);
        
        long totalAnnotations = analyticsRepository.countTotalAnnotationsByProject(projectId);
        long acceptedAnnotations = analyticsRepository.countAcceptedAnnotationsByProject(projectId);
        long rejectedAnnotations = totalAnnotations - acceptedAnnotations;
        long totalViolations = analyticsRepository.countPolicyViolationsByProject(projectId);
        long totalLabelsUsed = analyticsRepository.countDistinctLabelsUsed(projectId);
        long improvementsFound = analyticsRepository.countImprovementsByProject(projectId);
        
        double annotationAccuracy = totalAnnotations > 0 ? 
                (double) acceptedAnnotations / totalAnnotations * 100 : 0;
        double policyComplianceRate = totalAnnotations > 0 ? 
                ((double) (totalAnnotations - totalViolations) / totalAnnotations) * 100 : 100;
        double improvementRate = totalAnnotations > 0 ? 
                (double) improvementsFound / totalAnnotations * 100 : 0;
        
        // Tính overall quality score
        double overallScore = (annotationAccuracy * 0.5 + policyComplianceRate * 0.3 + improvementRate * 0.2) / 100 * 100;
        String qualityLevel = determineQualityLevel(overallScore);
        
        return QualityMetricsResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .annotationAccuracy(Math.min(annotationAccuracy, 100.0))
                .totalAnnotations(totalAnnotations)
                .acceptedAnnotations(acceptedAnnotations)
                .rejectedAnnotations(rejectedAnnotations)
                .policyComplianceRate(Math.min(policyComplianceRate, 100.0))
                .totalPolicyViolations(totalViolations)
                .criticalViolations(totalViolations / 3) // Ước tính
                .minorViolations(totalViolations - totalViolations / 3)
                .totalLabelUsed((int) totalLabelsUsed)
                .labelDistributionBalance(calculateLabelDistributionBalance(projectId))
                .totalReviewsCompleted(acceptedAnnotations + rejectedAnnotations)
                .improvementsFound(improvementsFound)
                .improvementRate(Math.min(improvementRate, 100.0))
                .overallQualityScore(Math.min(overallScore, 100.0))
                .qualityLevel(qualityLevel)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    /**
     * Lấy danh sách đóng góp của các thành viên
     */
    public List<ContributionResponse> getTeamContributions(Long projectId) {
        Project project = getProjectAndValidateAccess(projectId);
        
        List<Assignment> assignments = assignmentRepository.findByProject_ProjectId(projectId);
        
        Map<User, ContributionMetrics> userMetrics = new HashMap<>();
        
        for (Assignment assignment : assignments) {
            // Thống kê cho Annotator
            User annotator = assignment.getAnnotator();
            userMetrics.computeIfAbsent(annotator, u -> new ContributionMetrics(u, "ANNOTATOR"))
                    .addAssignment(assignment);
            
            // Thống kê cho Reviewer
            if (assignment.getReviewer() != null) {
                User reviewer = assignment.getReviewer();
                userMetrics.computeIfAbsent(reviewer, u -> new ContributionMetrics(u, "REVIEWER"))
                        .incrementReviews();
            }
        }
        
        return userMetrics.values().stream()
                .map(this::mapToContributionResponse)
                .sorted(Comparator.comparingDouble(ContributionResponse::getPerformanceScore).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy thông tin chi tiết đóng góp của một người dùng
     */
    public ContributionResponse getUserContribution(Long projectId, Long userId) {
        Project project = getProjectAndValidateAccess(projectId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        long totalAssignments = analyticsRepository.countTotalAssignmentsByUser(userId);
        long completedAssignments = analyticsRepository.countCompletedAssignmentsByUser(userId);
        double completionRate = totalAssignments > 0 ? 
                (double) completedAssignments / totalAssignments * 100 : 0;
        
        long annotationsCount = analyticsRepository.countAnnotationsByUser(userId);
        long policiesViolated = analyticsRepository.countPolicyViolationsByUser(userId);
        double policyComplianceRate = annotationsCount > 0 ? 
                ((double) (annotationsCount - policiesViolated) / annotationsCount) * 100 : 100;
        
        long reviewsCount = analyticsRepository.countReviewsByUser(userId);
        long approvedCount = analyticsRepository.countApprovedReviewsByUser(userId);
        long rejectedCount = analyticsRepository.countRejectedReviewsByUser(userId);
        double rejectionRate = reviewsCount > 0 ? 
                (double) rejectedCount / reviewsCount * 100 : 0;
        
        double performanceScore = calculatePerformanceScore(completionRate, policyComplianceRate, 
                reviewsCount > 0 ? (100 - rejectionRate) : completionRate);
        
        return ContributionResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .totalAssignments(totalAssignments)
                .completedAssignments(completedAssignments)
                .completionRate(Math.min(completionRate, 100.0))
                .annotationsCount(annotationsCount)
                .policiesViolated(policiesViolated)
                .policyComplianceRate(Math.min(policyComplianceRate, 100.0))
                .reviewsCount(reviewsCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .rejectionRate(rejectionRate)
                .performanceScore(Math.min(performanceScore, 100.0))
                .build();
    }
    
    /**
     * Lấy chất lượng các thành phần (label, policy, etc.)
     */
    public List<ComponentQualityResponse> getComponentQuality(Long projectId) {
        Project project = getProjectAndValidateAccess(projectId);
        
        List<Dataset> datasets = datasetRepository.findByProject_ProjectId(projectId);
        List<Label> labels = labelRepository.findByIsActiveTrue();
        
        List<ComponentQualityResponse> responses = new ArrayList<>();
        
        // Tính chất lượng cho các Label
        for (Label label : labels) {
            responses.add(calculateLabelQuality(label, projectId));
        }
        
        return responses;
    }
    
    /**
     * Lấy tóm tắt phân tích dự án
     */
    public ProjectAnalyticsSummaryResponse getProjectAnalyticsSummary(Long projectId) {
        ProjectProgressResponse progress = getProjectProgress(projectId);
        QualityMetricsResponse qualityMetrics = getProjectQualityMetrics(projectId);
        List<ContributionResponse> allContributions = getTeamContributions(projectId);
        
        List<ContributionResponse> topContributors = allContributions.stream()
                .limit(5)
                .collect(Collectors.toList());
        
        double averagePerformance = allContributions.isEmpty() ? 0 :
                allContributions.stream()
                        .mapToDouble(ContributionResponse::getPerformanceScore)
                        .average()
                        .orElse(0);
        
        List<String> alerts = generateAlerts(progress, qualityMetrics, allContributions);
        
        return ProjectAnalyticsSummaryResponse.builder()
                .projectId(projectId)
                .projectName(progress.getProjectName())
                .status(progress.getStatus())
                .progress(progress)
                .qualityMetrics(qualityMetrics)
                .topContributors(topContributors)
                .totalTeamMembers(allContributions.size())
                .teamAveragePerformanceScore(Math.min(averagePerformance, 100.0))
                .alerts(alerts)
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Private helper methods
     */
    
    private Project getProjectAndValidateAccess(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Only manager can access analytics
        if (!project.getManager().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Only project manager can access analytics");
        }
        
        return project;
    }
    
    private String determineQualityLevel(double score) {
        if (score >= 80) return "EXCELLENT";
        if (score >= 60) return "GOOD";
        if (score >= 40) return "FAIR";
        return "POOR";
    }
    
    private double calculateLabelDistributionBalance(Long projectId) {
        List<Object[]> distribution = analyticsRepository.getLabelDistributionByProject(projectId);
        
        if (distribution.isEmpty()) return 0;
        
        List<Long> counts = distribution.stream()
                .map(arr -> ((Number) arr[1]).longValue())
                .collect(Collectors.toList());
        
        long total = counts.stream().mapToLong(Long::longValue).sum();
        long expectedPerLabel = total / distribution.size();
        
        double variance = counts.stream()
                .mapToDouble(count -> Math.pow(count - expectedPerLabel, 2))
                .average()
                .orElse(0);
        
        // Normalize to 0-100
        double stdDev = Math.sqrt(variance);
        double balance = Math.max(0, 100 - (stdDev / expectedPerLabel * 100));
        
        return Math.min(balance, 100.0);
    }
    
    private double calculatePerformanceScore(double... scores) {
        return Arrays.stream(scores).average().orElse(0);
    }
    
    private ContributionResponse mapToContributionResponse(ContributionMetrics metrics) {
        User user = metrics.user;
        
        double performanceScore = calculatePerformanceScore(
                metrics.completionRate,
                metrics.policyComplianceRate,
                metrics.reviewsCount > 0 ? (100 - metrics.rejectionRate) : metrics.completionRate
        );
        
        return ContributionResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(metrics.role)
                .totalAssignments(metrics.totalAssignments)
                .completedAssignments(metrics.completedAssignments)
                .completionRate(Math.min(metrics.completionRate, 100.0))
                .annotationsCount(metrics.annotationsCount)
                .policiesViolated(metrics.policiesViolated)
                .policyComplianceRate(Math.min(metrics.policyComplianceRate, 100.0))
                .reviewsCount(metrics.reviewsCount)
                .approvedCount(metrics.approvedCount)
                .rejectedCount(metrics.rejectedCount)
                .rejectionRate(metrics.rejectionRate)
                .performanceScore(Math.min(performanceScore, 100.0))
                .build();
    }
    
    private ComponentQualityResponse calculateLabelQuality(Label label, Long projectId) {
        // Get usage statistics for this label
        List<Reviewing> reviews = reviewingRepository.findAll().stream()
                .filter(r -> r.getLabel().getLabelId().equals(label.getLabelId()))
                .filter(r -> r.getAssignment().getProject().getProjectId().equals(projectId))
                .collect(Collectors.toList());
        
        long usageCount = reviews.size();
        long approvedCount = reviews.stream().filter(r -> "APPROVED".equals(r.getStatus().toString())).count();
        long errorCount = reviews.stream().filter(r -> !"APPROVED".equals(r.getStatus().toString())).count();
        
        double accuracy = usageCount > 0 ? (double) approvedCount / usageCount * 100 : 0;
        String status = accuracy >= 80 ? "HEALTHY" : accuracy >= 60 ? "WARNING" : "CRITICAL";
        
        return ComponentQualityResponse.builder()
                .componentType("LABEL")
                .componentName(label.getLabelName())
                .componentId(label.getLabelId())
                .usageCount(usageCount)
                .errorCount(errorCount)
                .qualityScore(accuracy)
                .status(status)
                .accuracy(accuracy)
                .recommendation(generateLabelRecommendation(accuracy, usageCount))
                .build();
    }
    
    private String generateLabelRecommendation(double accuracy, long usageCount) {
        if (accuracy < 60 && usageCount > 100) {
            return "Label này có chất lượng thấp và được sử dụng nhiều. Cần review định nghĩa label.";
        } else if (accuracy >= 80) {
            return "Label này có chất lượng tốt.";
        } else {
            return "Cân nhắc cải tiến hướng dẫn annotator về label này.";
        }
    }
    
    private List<String> generateAlerts(ProjectProgressResponse progress, 
                                       QualityMetricsResponse quality,
                                       List<ContributionResponse> contributions) {
        List<String> alerts = new ArrayList<>();
        
        if (progress.getOverallProgress() < 20) {
            alerts.add("⚠️ Dự án chưa bắt đầu: Chỉ có " + progress.getOverallProgress() + "% hoàn thành");
        } else if (progress.getOverallProgress() < 50) {
            alerts.add("⚠️ Tiến độ dự án chậm: " + progress.getOverallProgress() + "% hoàn thành");
        }
        
        if (quality.getAnnotationAccuracy() < 70) {
            alerts.add("⚠️ Chất lượng annotation thấp: " + quality.getAnnotationAccuracy() + "%");
        }
        
        if (quality.getPolicyComplianceRate() < 80) {
            alerts.add("⚠️ Tỉ lệ tuân thủ chính sách: " + quality.getPolicyComplianceRate() + "%");
        }
        
        long lowPerformers = contributions.stream()
                .filter(c -> c.getPerformanceScore() < 50)
                .count();
        if (lowPerformers > 0) {
            alerts.add("⚠️ Có " + lowPerformers + " thành viên có hiệu suất thấp");
        }
        
        return alerts;
    }
    
    /**
     * Helper class tạm để lưu tữ metrics trong quá trình tính toán
     */
    private static class ContributionMetrics {
        User user;
        String role;
        long totalAssignments = 0;
        long completedAssignments = 0;
        long annotationsCount = 0;
        long policiesViolated = 0;
        long reviewsCount = 0;
        long approvedCount = 0;
        long rejectedCount = 0;
        
        double completionRate = 0;
        double policyComplianceRate = 100;
        double rejectionRate = 0;
        
        ContributionMetrics(User user, String role) {
            this.user = user;
            this.role = role;
        }
        
        void addAssignment(Assignment assignment) {
            totalAssignments++;
            if (assignment.getStatus().toString().equals("COMPLETED")) {
                completedAssignments++;
            }
            updateCompletionRate();
        }
        
        void incrementReviews() {
            reviewsCount++;
        }
        
        void updateCompletionRate() {
            completionRate = totalAssignments > 0 ?
                    (double) completedAssignments / totalAssignments * 100 : 0;
        }
    }
}

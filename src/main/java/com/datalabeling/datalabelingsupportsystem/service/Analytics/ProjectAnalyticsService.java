package com.datalabeling.datalabelingsupportsystem.service.Analytics;

import com.datalabeling.datalabelingsupportsystem.dto.response.Analytics.*;
import com.datalabeling.datalabelingsupportsystem.pojo.*;
import com.datalabeling.datalabelingsupportsystem.repository.Analytics.AnalyticsRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.AssignmentRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Labeling.ReviewingRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.ViolationRepository;
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
    private final ViolationRepository violationRepository;
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
        long totalViolations = violationRepository.countByProject_ProjectId(projectId);
        long totalLabelsUsed = analyticsRepository.countDistinctLabelsUsed(projectId);
        long improvementsFound = analyticsRepository.countImprovementsByProject(projectId);
        
        double annotationAccuracy = totalAnnotations > 0 ? 
                (double) acceptedAnnotations / totalAnnotations * 100 : 0;

        long distinctViolationReviewings = violationRepository.countDistinctReviewingViolationsByProject(projectId);
        long annotationWithoutViolation = Math.max(0, totalAnnotations - distinctViolationReviewings);

        double policyComplianceRate = totalAnnotations > 0 ? 
                (double) annotationWithoutViolation / totalAnnotations * 100 : 100;

        long criticalCount = violationRepository.countByProject_ProjectIdAndSeverity(projectId, 4);
        long highCount = violationRepository.countByProject_ProjectIdAndSeverity(projectId, 3);
        long mediumCount = violationRepository.countByProject_ProjectIdAndSeverity(projectId, 2);
        long lowCount = violationRepository.countByProject_ProjectIdAndSeverity(projectId, 1);

        double weightedViolationScore = highCount * 1.0 + mediumCount * 0.5 + lowCount * 0.2 + criticalCount * 1.5;
        double weightedComplianceAdjust = totalAnnotations > 0 ? Math.max(0, 100 - (weightedViolationScore / totalAnnotations * 100)) : 100;

        double improvementRate = totalAnnotations > 0 ? 
                (double) improvementsFound / totalAnnotations * 100 : 0;

        // Tính overall quality score kết hợp weighted compliance
        double overallScore = (annotationAccuracy * 0.45 + weightedComplianceAdjust * 0.35 + improvementRate * 0.2);
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
        getProjectAndValidateAccess(projectId);
        List<Assignment> assignments = assignmentRepository.findByProject_ProjectId(projectId);
        
        Map<Long, ContributionRoleContext> members = new LinkedHashMap<>();
        
        for (Assignment assignment : assignments) {
            User annotator = assignment.getAnnotator();
            members.putIfAbsent(
                    annotator.getUserId(),
                    new ContributionRoleContext(annotator, "ANNOTATOR")
            );
            
            if (assignment.getReviewer() != null) {
                User reviewer = assignment.getReviewer();
                members.putIfAbsent(
                        reviewer.getUserId(),
                        new ContributionRoleContext(reviewer, "REVIEWER")
                );
            }
        }
        
        return members.values().stream()
                .map(member -> buildContributionResponse(projectId, member.user(), member.role(), assignments))
                .sorted(Comparator.comparingDouble(ContributionResponse::getPerformanceScore).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy thông tin chi tiết đóng góp của một người dùng
     */
    public ContributionResponse getUserContribution(Long projectId, Long userId) {
        getProjectAndValidateAccess(projectId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Assignment> projectAssignments = assignmentRepository.findByProject_ProjectId(projectId);

        String role = projectAssignments.stream()
                .anyMatch(a -> a.getReviewer() != null && a.getReviewer().getUserId().equals(userId))
                ? "REVIEWER"
                : "ANNOTATOR";

        return buildContributionResponse(projectId, user, role, projectAssignments);
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
     * Lấy tóm tắt thống kê violation
     */
    public ViolationSummaryResponse getViolationSummary(Long projectId) {
        Project project = getProjectAndValidateAccess(projectId);

        long totalViolations = violationRepository.countByProject_ProjectId(projectId);
        long typeWrong = violationRepository.countByProject_ProjectIdAndViolationType(projectId, com.datalabeling.datalabelingsupportsystem.enums.Policies.ViolationType.WRONG_LABEL);
        long typeMissing = violationRepository.countByProject_ProjectIdAndViolationType(projectId, com.datalabeling.datalabelingsupportsystem.enums.Policies.ViolationType.MISSING_LABEL);
        long typePolicy = violationRepository.countByProject_ProjectIdAndViolationType(projectId, com.datalabeling.datalabelingsupportsystem.enums.Policies.ViolationType.POLICY_VIOLATION);
        long typeFormat = violationRepository.countByProject_ProjectIdAndViolationType(projectId, com.datalabeling.datalabelingsupportsystem.enums.Policies.ViolationType.FORMAT_ERROR);

        // by user
        Map<Long, Long> byUser = new HashMap<>();
        List<Object[]> userCounts = violationRepository.countByProject_ProjectIdGroupByAnnotator(projectId);
        for (Object[] row : userCounts) {
            Long userId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            byUser.put(userId, count);
        }

        Map<String, Long> byType = Map.of(
                "WRONG_LABEL", typeWrong,
                "MISSING_LABEL", typeMissing,
                "POLICY_VIOLATION", typePolicy,
                "FORMAT_ERROR", typeFormat
        );

        return ViolationSummaryResponse.builder()
                .projectId(projectId)
                .total(totalViolations)
                .byType(byType)
                .byUser(byUser)
                .build();
    }

    /**
     * Lấy danh sách violation dự án
     */
    public List<Violation> getProjectViolations(Long projectId) {
        getProjectAndValidateAccess(projectId);
        return violationRepository.findAll().stream()
                .filter(v -> v.getProject().getProjectId().equals(projectId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết violation
     */
    public Violation getProjectViolation(Long projectId, Long violationId) {
        getProjectAndValidateAccess(projectId);
        return violationRepository.findById(violationId)
                .filter(v -> v.getProject().getProjectId().equals(projectId))
                .orElseThrow(() -> new RuntimeException("Violation not found"));
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
        double expectedPerLabel = (double) total / distribution.size();
        
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
    
    private ContributionResponse buildContributionResponse(Long projectId, User user, String role, List<Assignment> projectAssignments) {
        List<Assignment> userAssignments = projectAssignments.stream()
                .filter(assignment -> "REVIEWER".equals(role)
                        ? assignment.getReviewer() != null && assignment.getReviewer().getUserId().equals(user.getUserId())
                        : assignment.getAnnotator() != null && assignment.getAnnotator().getUserId().equals(user.getUserId()))
                .toList();

        long totalAssignments = userAssignments.size();
        long completedAssignments = userAssignments.stream()
                .filter(assignment -> isCompletedForRole(assignment, role))
                .count();
        double completionRate = totalAssignments > 0 ? (double) completedAssignments / totalAssignments * 100 : 0;

        long annotationsCount = analyticsRepository.countAnnotationsByUserInProject(projectId, user.getUserId());
        long approvedAnnotations = analyticsRepository.countApprovedAnnotationsByUserInProject(projectId, user.getUserId());
        long policiesViolated = violationRepository.countByProject_ProjectIdAndAnnotator_UserId(projectId, user.getUserId());
        double annotationQuality = annotationsCount > 0 ? (double) approvedAnnotations / annotationsCount * 100 : 0;
        double policyComplianceRate = annotationsCount > 0
                ? ((double) (annotationsCount - policiesViolated) / annotationsCount) * 100
                : 0;

        long reviewsCount = analyticsRepository.countReviewsByUserInProject(projectId, user.getUserId());
        long approvedCount = analyticsRepository.countApprovedReviewsByUserInProject(projectId, user.getUserId());
        long rejectedCount = analyticsRepository.countRejectedReviewsByUserInProject(projectId, user.getUserId());
        double rejectionRate = reviewsCount > 0 ? (double) rejectedCount / reviewsCount * 100 : 0;

        double qualityScore = "REVIEWER".equals(role)
                ? (reviewsCount > 0 ? (100 - rejectionRate) : 0)
                : annotationQuality;
        double complianceScore = "REVIEWER".equals(role)
                ? (reviewsCount > 0 ? 100 : 0)
                : policyComplianceRate;
        double performanceScore = calculatePerformanceScore(completionRate, complianceScore, qualityScore);

        return ContributionResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(role)
                .totalAssignments(totalAssignments)
                .completedAssignments(completedAssignments)
                .completionRate(Math.min(completionRate, 100.0))
                .annotationsCount(annotationsCount)
                .annotationQuality(Math.min(annotationQuality, 100.0))
                .policiesViolated(policiesViolated)
                .policyComplianceRate(Math.min(policyComplianceRate, 100.0))
                .reviewsCount(reviewsCount)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .rejectionRate(Math.min(rejectionRate, 100.0))
                .performanceScore(Math.min(performanceScore, 100.0))
                .build();
    }

    private boolean isCompletedForRole(Assignment assignment, String role) {
        if ("REVIEWER".equals(role)) {
            return assignment.getStatus() == com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus.APPROVED
                    || assignment.getStatus() == com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus.REJECTED;
        }

        return assignment.getStatus() == com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus.COMPLETED
                || assignment.getStatus() == com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus.SUBMITTED
                || assignment.getStatus() == com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus.RE_SUBMITTED
                || assignment.getStatus() == com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus.APPROVED
                || assignment.getStatus() == com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus.REJECTED;
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
     * Lấy danh sách điểm số của các thành viên trong dự án (được xếp hạng)
     */
    public List<MemberScoreResponse> getMemberScores(Long projectId) {
        Project project = getProjectAndValidateAccess(projectId);
        
        List<ContributionResponse> contributions = getTeamContributions(projectId);
        
        // Tạo MemberScoreResponse từ ContributionResponse và thêm xếp hạng
        List<MemberScoreResponse> scores = new ArrayList<>();
        for (int i = 0; i < contributions.size(); i++) {
            ContributionResponse contrib = contributions.get(i);
            MemberScoreResponse score = mapToMemberScore(contrib, i + 1, contributions.size());
            scores.add(score);
        }
        
        return scores;
    }

    /**
     * Lấy chi tiết điểm số của một thành viên cụ thể
     */
    public MemberScoreResponse getMemberScore(Long projectId, Long userId) {
        Project project = getProjectAndValidateAccess(projectId);
        ContributionResponse contribution = getUserContribution(projectId, userId);
        
        // Lấy tất cả các thành viên để xác định xếp hạng
        List<ContributionResponse> allContributions = getTeamContributions(projectId);
        int rank = 1;
        for (ContributionResponse c : allContributions) {
            if (c.getUserId().equals(userId)) {
                break;
            }
            rank++;
        }
        
        return mapToMemberScore(contribution, rank, allContributions.size());
    }

    /**
     * Helper: Chuyển ContributionResponse sang MemberScoreResponse và thêm xếp hạng
     */
    private MemberScoreResponse mapToMemberScore(ContributionResponse contrib, int rank, int totalMembers) {
        double completionRate = contrib.getCompletionRate() != null ? contrib.getCompletionRate() : 0;
        double qualityScore = contrib.getAnnotationQuality() != null ? contrib.getAnnotationQuality() : 
                              (contrib.getRejectionRate() != null ? (100 - contrib.getRejectionRate()) : contrib.getPerformanceScore());
        double complianceScore = contrib.getPolicyComplianceRate() != null ? contrib.getPolicyComplianceRate() : 100;
        
        String tier = determineTier(contrib.getPerformanceScore());
        
        return MemberScoreResponse.builder()
                .userId(contrib.getUserId())
                .username(contrib.getUsername())
                .fullName(contrib.getFullName())
                .role(contrib.getRole())
                .performanceScore(Math.min(Math.max(contrib.getPerformanceScore(), 0), 100))
                .completionRate(Math.min(Math.max(completionRate, 0), 100))
                .qualityScore(Math.min(Math.max(qualityScore, 0), 100))
                .complianceScore(Math.min(Math.max(complianceScore, 0), 100))
                .rank(rank)
                .tier(tier)
                .totalAssignments(contrib.getTotalAssignments())
                .completedAssignments(contrib.getCompletedAssignments())
                .annotationsCount(contrib.getAnnotationsCount())
                .reviewsCount(contrib.getReviewsCount())
                .build();
    }

    /**
     * Helper: Xác định tier dựa vào performance score
     */
    private String determineTier(Double score) {
        if (score == null) score = 0.0;
        if (score >= 85) return "EXCELLENT";
        if (score >= 70) return "GOOD";
        if (score >= 50) return "AVERAGE";
        return "POOR";
    }
    
    private record ContributionRoleContext(User user, String role) {}
}

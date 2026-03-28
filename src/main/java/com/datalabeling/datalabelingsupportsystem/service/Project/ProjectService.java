package com.datalabeling.datalabelingsupportsystem.service.Project;

import com.datalabeling.datalabelingsupportsystem.dto.request.Project.CreateProjectRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Project.UpdateProjectRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Project.ProjectResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.AssignmentRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final DatasetRepository datasetRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (!"MANAGER".equals(manager.getRole().getRoleName())) {
            throw new RuntimeException("Only MANAGER can create projects");
        }

        if (projectRepository.existsByNameAndManagerUserId(request.getName(), manager.getUserId())) {
            throw new RuntimeException("Project name already exists for this manager");
        }

        Project project = Project.builder()
                .name(request.getName())
                .dataType(request.getDataType())
                .description(request.getDescription())
                .guidelineContent(request.getGuidelineContent())
                .guidelineVersion(request.getGuidelineVersion())
                .guidelineFileUrl(request.getGuidelineFileUrl())
                .status("DRAFT")
                .manager(manager)
                .createdAt(LocalDateTime.now())
                .build();

        Project savedProject = projectRepository.save(project);

        return mapToResponse(savedProject);
    }

    public List<ProjectResponse> getMyProjects() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepository.findByManagerUserId(manager.getUserId());
        Map<Long, ProjectSummaryMetrics> summaryByProjectId = buildProjectSummaryMetrics(projects);

        // Lấy tất cả projects (bao gồm INACTIVE) để hiện option kích hoạt lại
        return projects.stream()
                .map(project -> mapToResponse(project, summaryByProjectId.get(project.getProjectId())))
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Kiểm tra quyền: chỉ manager của project mới được xem
        if (!project.getManager().getUserId().equals(manager.getUserId())) {
            throw new RuntimeException("You don't have permission to view this project");
        }

        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Kiểm tra quyền: chỉ manager của project mới được xóa
        if (!project.getManager().getUserId().equals(manager.getUserId())) {
            throw new RuntimeException("You don't have permission to delete this project");
        }

        // Kiểm tra project status: không được xóa project COMPLETED
        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new RuntimeException("Cannot delete COMPLETED project. Please create a new project or resume from DRAFT status if needed.");
        }

        // Kiểm tra xem project có assignments hay không
        long assignmentCount = assignmentRepository.findByProject_ProjectId(projectId).size();
        if (assignmentCount > 0) {
            throw new RuntimeException("Cannot delete project with active assignments. Please remove all assignments first. Current assignments: " + assignmentCount);
        }

        // Soft delete: đổi status thành INACTIVE thay vì xóa hẳn
        project.setStatus("INACTIVE");
        projectRepository.save(project);
    }

    @Transactional
    public ProjectResponse updateProjectStatus(Long projectId, String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Kiểm tra quyền
        if (!project.getManager().getUserId().equals(manager.getUserId())) {
            throw new RuntimeException("You don't have permission to update this project");
        }

        // Validate status: chỉ cho phép DRAFT, IN_PROGRESS, PAUSED, COMPLETED
        if (!List.of("DRAFT", "IN_PROGRESS", "PAUSED", "COMPLETED").contains(status)) {
            throw new RuntimeException("Invalid status");
        }

        // Validate status transition
        String currentStatus = project.getStatus();
        if (!isValidStatusTransition(currentStatus, status)) {
            throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + status);
        }

        project.setStatus(status);
        project = projectRepository.save(project);

        return mapToResponse(project);
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // DRAFT có thể chuyển sang IN_PROGRESS
        if ("DRAFT".equals(currentStatus)) {
            return "IN_PROGRESS".equals(newStatus);
        }
        // IN_PROGRESS có thể chuyển sang PAUSED
        if ("IN_PROGRESS".equals(currentStatus)) {
            return "PAUSED".equals(newStatus);
        }
        // PAUSED có thể chuyển lại IN_PROGRESS (resume)
        if ("PAUSED".equals(currentStatus)) {
            return "IN_PROGRESS".equals(newStatus);
        }
        // COMPLETED không thể thay đổi
        return false;
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Kiểm tra quyền
        if (!project.getManager().getUserId().equals(manager.getUserId())) {
            throw new RuntimeException("You don't have permission to update this project");
        }

        // Chỉ cho phép update project ở trạng thái DRAFT, IN_PROGRESS, PAUSED
        String currentStatus = project.getStatus();
        if (!List.of("DRAFT", "IN_PROGRESS", "PAUSED").contains(currentStatus)) {
            throw new RuntimeException("Cannot update project in " + currentStatus + " status. Only DRAFT, IN_PROGRESS, and PAUSED projects can be updated.");
        }

        // Update các trường nếu có giá trị mới
        if (request.getName() != null && !request.getName().isBlank()) {
            // Kiểm tra tên project mới có bị trùng không (trừ chính nó)
            if (!request.getName().equals(project.getName()) &&
                    projectRepository.existsByNameAndManagerUserId(request.getName(), manager.getUserId())) {
                throw new RuntimeException("Project name already exists for this manager");
            }
            project.setName(request.getName());
        }

        if (request.getDataType() != null && !request.getDataType().isBlank()) {
            project.setDataType(request.getDataType());
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        if (request.getGuidelineContent() != null) {
            project.setGuidelineContent(request.getGuidelineContent());
        }

        if (request.getGuidelineVersion() != null) {
            project.setGuidelineVersion(request.getGuidelineVersion());
        }

        if (request.getGuidelineFileUrl() != null) {
            project.setGuidelineFileUrl(request.getGuidelineFileUrl());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            // Validate status
            if (!List.of("DRAFT", "IN_PROGRESS", "PAUSED", "COMPLETED").contains(request.getStatus())) {
                throw new RuntimeException("Invalid status");
            }
            // Validate status transition
            if (!isValidStatusTransition(currentStatus, request.getStatus())) {
                throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + request.getStatus());
            }
            project.setStatus(request.getStatus());
        }

        project = projectRepository.save(project);

        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse activateProject(Long projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Kiểm tra quyền: chỉ manager của project mới được kích hoạt lại
        if (!project.getManager().getUserId().equals(manager.getUserId())) {
            throw new RuntimeException("You don't have permission to activate this project");
        }

        // Kiểm tra project phải ở trạng thái INACTIVE
        if (!"INACTIVE".equals(project.getStatus())) {
            throw new RuntimeException("Only INACTIVE projects can be activated");
        }

        // Kích hoạt lại: đổi status thành DRAFT
        project.setStatus("DRAFT");
        project = projectRepository.save(project);

        return mapToResponse(project);
    }

    private ProjectResponse mapToResponse(Project project) {
        return mapToResponse(project, null);
    }

    private ProjectResponse mapToResponse(Project project, ProjectSummaryMetrics summary) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .name(project.getName())
                .dataType(project.getDataType())
                .status(project.getStatus())
                .computedDisplayStatus(summary != null
                        ? summary.computedDisplayStatus
                        : normalizeProjectStatus(project.getStatus()))
                .description(project.getDescription())
                .guidelineContent(project.getGuidelineContent())
                .guidelineVersion(project.getGuidelineVersion())
                .guidelineFileUrl(project.getGuidelineFileUrl())
                .managerName(project.getManager().getFullName() != null
                        ? project.getManager().getFullName()
                        : project.getManager().getUsername())
                .managerId(project.getManager().getUserId())
                .createdAt(project.getCreatedAt())
                .assignmentCount(summary != null ? summary.assignmentCount : null)
                .approvedAssignmentCount(summary != null ? summary.approvedAssignmentCount : null)
                .inProgressAssignmentCount(summary != null ? summary.inProgressAssignmentCount : null)
                .rejectedAssignmentCount(summary != null ? summary.rejectedAssignmentCount : null)
                .datasetCount(summary != null ? summary.datasetCount : null)
                .build();
    }

    private Map<Long, ProjectSummaryMetrics> buildProjectSummaryMetrics(List<Project> projects) {
        Map<Long, ProjectSummaryMetrics> metricsByProjectId = new HashMap<>();
        if (projects.isEmpty()) {
            return metricsByProjectId;
        }

        List<Long> projectIds = projects.stream()
                .map(Project::getProjectId)
                .toList();

        Map<Long, Long> datasetCountByProjectId = new HashMap<>();
        datasetRepository.countByProjectIds(projectIds)
                .forEach(row -> datasetCountByProjectId.put(row.getProjectId(), row.getTotal()));

        Map<Long, Map<AssignmentStatus, Long>> assignmentStatusByProjectId = new HashMap<>();
        assignmentRepository.countStatusesByProjectIds(projectIds)
                .forEach(row -> assignmentStatusByProjectId
                        .computeIfAbsent(row.getProjectId(), ignored -> new HashMap<>())
                        .put(row.getStatus(), row.getTotal()));

        for (Project project : projects) {
            Map<AssignmentStatus, Long> statusCounts = assignmentStatusByProjectId.getOrDefault(
                    project.getProjectId(),
                    Map.of()
            );
            long approvedCount = statusCounts.getOrDefault(AssignmentStatus.APPROVED, 0L)
                    + statusCounts.getOrDefault(AssignmentStatus.COMPLETED, 0L);
            long inProgressCount = statusCounts.getOrDefault(AssignmentStatus.IN_PROGRESS, 0L)
                    + statusCounts.getOrDefault(AssignmentStatus.SUBMITTED, 0L)
                    + statusCounts.getOrDefault(AssignmentStatus.RE_SUBMITTED, 0L);
            long rejectedCount = statusCounts.getOrDefault(AssignmentStatus.REJECTED, 0L);
            long assignmentCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();
            long datasetCount = datasetCountByProjectId.getOrDefault(project.getProjectId(), 0L);

            metricsByProjectId.put(project.getProjectId(), new ProjectSummaryMetrics(
                    assignmentCount,
                    approvedCount,
                    inProgressCount,
                    rejectedCount,
                    datasetCount,
                    computeDisplayStatus(project.getStatus(), assignmentCount, approvedCount, inProgressCount, rejectedCount)
            ));
        }

        return metricsByProjectId;
    }

    private String computeDisplayStatus(
            String projectStatus,
            long assignmentCount,
            long approvedCount,
            long inProgressCount,
            long rejectedCount
    ) {
        String normalizedStatus = normalizeProjectStatus(projectStatus);
        if ("PAUSED".equals(normalizedStatus) || "INACTIVE".equals(normalizedStatus) || "COMPLETED".equals(normalizedStatus)) {
            return normalizedStatus;
        }
        if (assignmentCount == 0) {
            return "DRAFT";
        }
        if (approvedCount == assignmentCount) {
            return "COMPLETED";
        }
        if (rejectedCount > 0 || inProgressCount > 0) {
            return "IN_PROGRESS";
        }
        return "DRAFT";
    }

    private String normalizeProjectStatus(String status) {
        return status == null ? "DRAFT" : status.trim().toUpperCase();
    }

    private static final class ProjectSummaryMetrics {
        private final Long assignmentCount;
        private final Long approvedAssignmentCount;
        private final Long inProgressAssignmentCount;
        private final Long rejectedAssignmentCount;
        private final Long datasetCount;
        private final String computedDisplayStatus;

        private ProjectSummaryMetrics(
                Long assignmentCount,
                Long approvedAssignmentCount,
                Long inProgressAssignmentCount,
                Long rejectedAssignmentCount,
                Long datasetCount,
                String computedDisplayStatus
        ) {
            this.assignmentCount = assignmentCount;
            this.approvedAssignmentCount = approvedAssignmentCount;
            this.inProgressAssignmentCount = inProgressAssignmentCount;
            this.rejectedAssignmentCount = rejectedAssignmentCount;
            this.datasetCount = datasetCount;
            this.computedDisplayStatus = computedDisplayStatus;
        }
    }
}

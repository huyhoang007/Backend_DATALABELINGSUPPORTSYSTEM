package com.datalabeling.datalabelingsupportsystem.service.Project;

import com.datalabeling.datalabelingsupportsystem.dto.request.Project.CreateProjectRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Project.UpdateProjectRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Project.ProjectResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

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

        // Chỉ lấy các project có status hợp lệ (không lấy project INACTIVE - đã xóa mềm)
        return projects.stream()
                .filter(project -> !"INACTIVE".equals(project.getStatus()))
                .map(this::mapToResponse)
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
        // IN_PROGRESS có thể chuyển sang PAUSED hoặc COMPLETED
        if ("IN_PROGRESS".equals(currentStatus)) {
            return "PAUSED".equals(newStatus) || "COMPLETED".equals(newStatus);
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

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .name(project.getName())
                .dataType(project.getDataType())
                .status(project.getStatus())
                .description(project.getDescription())
                .managerName(project.getManager().getFullName() != null
                        ? project.getManager().getFullName()
                        : project.getManager().getUsername())
                .managerId(project.getManager().getUserId())
                .createdAt(project.getCreatedAt())
                .build();
    }
}

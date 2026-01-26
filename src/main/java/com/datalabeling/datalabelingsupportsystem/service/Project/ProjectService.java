package com.datalabeling.datalabelingsupportsystem.service.Project;

import com.datalabeling.datalabelingsupportsystem.dto.request.Project.CreateProjectRequest;
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
                .status("ACTIVE")
                .manager(manager)
                .createdAt(LocalDateTime.now())
                .build();

        project = projectRepository.save(project);

        return mapToResponse(project);
    }

    public List<ProjectResponse> getMyProjects() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepository.findByManagerUserId(manager.getUserId());

        return projects.stream()
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
        if (!project.getManager().getUserId().equals(manager.getUserId())
                && !"ADMIN".equals(manager.getRole().getRoleName())) {
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

        projectRepository.delete(project);
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

        // Validate status
        if (!List.of("ACTIVE", "INACTIVE", "COMPLETED").contains(status)) {
            throw new RuntimeException("Invalid status");
        }

        project.setStatus(status);
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

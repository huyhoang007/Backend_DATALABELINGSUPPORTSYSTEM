package com.datalabeling.datalabelingsupportsystem.controller.Project;

import com.datalabeling.datalabelingsupportsystem.dto.request.Project.CreateProjectRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Project.SetProjectLabelRulesRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Project.UpdateProjectRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelRuleResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Project.ProjectResponse;
import com.datalabeling.datalabelingsupportsystem.service.Project.ProjectLabelRuleService;
import com.datalabeling.datalabelingsupportsystem.service.Project.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Project Management", description = "APIs for managing projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectLabelRuleService projectLabelRuleService;

    @Operation(summary = "Create new project", description = "Manager creates a new project")
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get my projects", description = "Get all projects of current manager")
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/my-projects")
    public ResponseEntity<List<ProjectResponse>> getMyProjects() {
        List<ProjectResponse> projects = projectService.getMyProjects();
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "Get project by ID", description = "Get project details by ID")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        ProjectResponse response = projectService.getProjectById(projectId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete project", description = "Manager deletes their project")
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update project status", description = "Update project status (ACTIVE, INACTIVE, COMPLETED)")
    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/{projectId}/status")
    public ResponseEntity<ProjectResponse> updateProjectStatus(
            @PathVariable Long projectId,
            @RequestParam String status) {
        ProjectResponse response = projectService.updateProjectStatus(projectId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update project", description = "Manager updates project information (name, dataType, description, status)")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get label rules for project", description = "Returns label rules linked to a project")
    @PreAuthorize("hasAnyRole('MANAGER', 'ANNOTATOR')")
    @GetMapping("/{projectId}/label-rules")
    public ResponseEntity<List<LabelRuleResponse>> getProjectLabelRules(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectLabelRuleService.getProjectLabelRules(projectId));
    }

    @Operation(summary = "Set label rules for project", description = "Replaces all label-rule associations for a project")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{projectId}/label-rules")
    public ResponseEntity<Void> setProjectLabelRules(
            @PathVariable Long projectId,
            @RequestBody SetProjectLabelRulesRequest request) {
        projectLabelRuleService.setProjectLabelRules(projectId, request.getRuleIds());
        return ResponseEntity.noContent().build();
    }
}

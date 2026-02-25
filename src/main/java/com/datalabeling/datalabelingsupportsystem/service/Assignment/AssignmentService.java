package com.datalabeling.datalabelingsupportsystem.service.Assignment;

import com.datalabeling.datalabelingsupportsystem.dto.request.Assignment.CreateAssignmentRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Assignment.AssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;
import com.datalabeling.datalabelingsupportsystem.pojo.Assignment;
import com.datalabeling.datalabelingsupportsystem.pojo.Dataset;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.AssignmentRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;

    /**
     * Manager tạo phân công: chọn dataset, annotator, reviewer
     */
    @Transactional
    public AssignmentResponse createAssignment(Long projectId, CreateAssignmentRequest request, Long managerId) {

        // Kiểm tra project tồn tại và manager có quyền
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (!project.getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("You are not the manager of this project");
        }

        // Kiểm tra dataset thuộc project
        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + request.getDatasetId()));

        if (!dataset.getProject().getProjectId().equals(projectId)) {
            throw new RuntimeException("Dataset does not belong to this project");
        }

        // Kiểm tra annotator
        User annotator = userRepository.findById(request.getAnnotatorId())
                .orElseThrow(() -> new RuntimeException("Annotator not found: " + request.getAnnotatorId()));

        if (!"ANNOTATOR".equalsIgnoreCase(annotator.getRole().getRoleName())) {
            throw new RuntimeException("User " + annotator.getUsername() + " is not an ANNOTATOR");
        }

        // Kiểm tra reviewer
        User reviewer = userRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + request.getReviewerId()));

        if (!"REVIEWER".equalsIgnoreCase(reviewer.getRole().getRoleName())) {
            throw new RuntimeException("User " + reviewer.getUsername() + " is not a REVIEWER");
        }

        // Kiểm tra dataset đã được assign chưa
        if (assignmentRepository.existsByDataset_DatasetIdAndAnnotator_UserId(
                request.getDatasetId(), request.getAnnotatorId())) {
            throw new RuntimeException("This dataset is already assigned to this annotator");
        }

        Assignment assignment = Assignment.builder()
                .project(project)
                .dataset(dataset)
                .annotator(annotator)
                .reviewer(reviewer)
                .status(AssignmentStatus.PENDING)
                .progress(0.0)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        return mapToResponse(saved);
    }

    /**
     * Manager xem danh sách phân công trong project
     */
    public List<AssignmentResponse> getAssignmentsByProject(Long projectId, Long managerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (!project.getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("You are not the manager of this project");
        }

        return assignmentRepository.findByProject_ProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Manager xóa phân công (chỉ khi status PENDING)
     */
    @Transactional
    public void deleteAssignment(Long assignmentId, Long managerId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        if (!assignment.getProject().getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("You are not the manager of this project");
        }

        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new RuntimeException("Cannot delete assignment with status: " + assignment.getStatus());
        }

        assignmentRepository.delete(assignment);
    }

    private AssignmentResponse mapToResponse(Assignment a) {
        return AssignmentResponse.builder()
                .assignmentId(a.getAssignmentId())
                .projectId(a.getProject().getProjectId())
                .projectName(a.getProject().getName())
                .datasetId(a.getDataset().getDatasetId())
                .datasetName(a.getDataset().getName())
                .annotatorId(a.getAnnotator().getUserId())
                .annotatorName(a.getAnnotator().getFullName())
                .reviewerId(a.getReviewer().getUserId())
                .reviewerName(a.getReviewer().getFullName())
                .status(a.getStatus())
                .progress(a.getProgress())
                .completedAt(a.getCompletedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

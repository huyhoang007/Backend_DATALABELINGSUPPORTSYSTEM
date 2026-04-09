package com.datalabeling.datalabelingsupportsystem.service.Assignment;

import com.datalabeling.datalabelingsupportsystem.dto.request.Assignment.CreateAssignmentRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Assignment.AssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;
import com.datalabeling.datalabelingsupportsystem.enums.Reviewing.ReviewingStatus;
import com.datalabeling.datalabelingsupportsystem.pojo.Assignment;
import com.datalabeling.datalabelingsupportsystem.pojo.Dataset;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.pojo.Reviewing;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.AssignmentRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Labeling.ReviewingRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.ProjectPolicyRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectLabelRuleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final ProjectLabelRuleRepository projectLabelRuleRepository;
    private final ProjectPolicyRepository projectPolicyRepository;
    private final ReviewingRepository reviewingRepository;

    @Transactional
    public AssignmentResponse createAssignment(Long projectId, CreateAssignmentRequest request, Long managerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Du an khong duoc tim thay: " + projectId));

        if (!project.getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("Ban khong phai la quan ly cua du an nay");
        }

        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new RuntimeException("Du an da hoan thanh va bi khoa. Chi cho phep cac hoat dong xuat.");
        }

        boolean hasLabelRules = !projectLabelRuleRepository.findByProject_ProjectId(projectId).isEmpty();
        boolean hasPolicies = !projectPolicyRepository.findByProject(project).isEmpty();

        if (!hasLabelRules) {
            throw new RuntimeException("Vui long them quy tac gan nhan cho du an nay truoc khi giao nhiem vu.");
        }
        if (!hasPolicies) {
            throw new RuntimeException("Vui long them chinh sach cho du an nay truoc khi giao nhiem vu.");
        }

        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new RuntimeException("Bo du lieu khong duoc tim thay: " + request.getDatasetId()));

        if (!dataset.getProject().getProjectId().equals(projectId)) {
            throw new RuntimeException("Bo du lieu khong thuoc du an nay");
        }

        User annotator = userRepository.findById(request.getAnnotatorId())
                .orElseThrow(() -> new RuntimeException("Nguoi chu thich khong duoc tim thay: " + request.getAnnotatorId()));

        if (!"ANNOTATOR".equalsIgnoreCase(annotator.getRole().getRoleName())) {
            throw new RuntimeException("Nguoi dung " + annotator.getUsername() + " khong phai la ANNOTATOR");
        }

        User reviewer = userRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new RuntimeException("Nguoi xem xet khong duoc tim thay: " + request.getReviewerId()));

        if (!"REVIEWER".equalsIgnoreCase(reviewer.getRole().getRoleName())) {
            throw new RuntimeException("Nguoi dung " + reviewer.getUsername() + " khong phai la REVIEWER");
        }

        if (assignmentRepository.existsByDataset_DatasetIdAndAnnotator_UserId(
                request.getDatasetId(), request.getAnnotatorId())) {
            throw new RuntimeException("Bo du lieu nay da duoc gan cho nguoi chu thich nay");
        }

        Assignment assignment = Assignment.builder()
                .project(project)
                .dataset(dataset)
                .annotator(annotator)
                .reviewer(reviewer)
                .status(AssignmentStatus.PENDING)
                .progress(0)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        return mapToResponse(saved);
    }

    public List<AssignmentResponse> getAssignmentsByProject(Long projectId, Long managerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Du an khong duoc tim thay: " + projectId));

        if (!project.getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("Ban khong phai la quan ly cua du an nay");
        }

        return assignmentRepository.findByProject_ProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAssignment(Long assignmentId, Long managerId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Phan cong khong duoc tim thay: " + assignmentId));

        if (!assignment.getProject().getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("Ban khong phai la quan ly cua du an nay");
        }

        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new RuntimeException("Khong the xoa phan cong voi trang thai: " + assignment.getStatus());
        }

        assignmentRepository.delete(assignment);
    }

    private AssignmentResponse mapToResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .projectId(assignment.getProject().getProjectId())
                .projectName(assignment.getProject().getName())
                .datasetId(assignment.getDataset().getDatasetId())
                .datasetName(assignment.getDataset().getName())
                .annotatorId(assignment.getAnnotator().getUserId())
                .annotatorName(assignment.getAnnotator().getFullName())
                .reviewerId(assignment.getReviewer().getUserId())
                .reviewerName(assignment.getReviewer().getFullName())
                .status(assignment.getStatus())
                .displayStatus(resolveManagerDisplayStatus(assignment))
                .progress(assignment.getProgress())
                .completedAt(assignment.getCompletedAt())
                .createdAt(assignment.getCreatedAt())
                .build();
    }

    private String resolveManagerDisplayStatus(Assignment assignment) {
        AssignmentStatus status = assignment.getStatus();
        if (status == null) {
            return "Cho xu ly";
        }

        Map<String, Reviewing> latestReviewings = reviewingRepository
                .findByAssignment_AssignmentId(assignment.getAssignmentId())
                .stream()
                .collect(Collectors.toMap(
                        review -> review.getDataItem().getItemId() + "::" + String.valueOf(review.getGeometry()),
                        review -> review,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new));

        boolean hasRejected = latestReviewings.values().stream()
                .anyMatch(review -> review.getStatus() == ReviewingStatus.REJECTED);
        boolean hasPendingReview = latestReviewings.values().stream()
                .anyMatch(review -> review.getStatus() == null || review.getStatus() == ReviewingStatus.PENDING);

        if (hasRejected) {
            return "Can sua";
        }

        if (hasPendingReview) {
            return switch (status) {
                case PENDING -> "Cho xu ly";
                case IN_PROGRESS -> "Dang xu ly";
                default -> "Cho duyet";
            };
        }

        return switch (status) {
            case PENDING -> "Cho xu ly";
            case IN_PROGRESS -> "Dang xu ly";
            case SUBMITTED, RE_SUBMITTED -> "Cho duyet";
            case REJECTED -> "Can sua";
            case APPROVED, COMPLETED -> "Hoan thanh";
        };
    }
}

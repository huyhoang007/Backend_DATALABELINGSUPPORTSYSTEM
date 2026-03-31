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
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectLabelRuleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.ProjectPolicyRepository;
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
    private final ProjectLabelRuleRepository projectLabelRuleRepository;
    private final ProjectPolicyRepository projectPolicyRepository;

    /**
     * Manager tạo phân công: chọn dataset, annotator, reviewer
     */
    @Transactional
    public AssignmentResponse createAssignment(Long projectId, CreateAssignmentRequest request, Long managerId) {

        // Kiểm tra project tồn tại và manager có quyền
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không được tìm thấy: " + projectId));

        if (!project.getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("Bạn không phải là quản lý của dự án này");
        }

        // Kiểm tra project status: không được phân công cho project COMPLETED
        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new RuntimeException("Dự án đã HOÀN THÀNH và bị khóa. Chỉ cho phép các hoạt động xuất.");
        }

        // Kiểm tra project có label rules và policies
        boolean hasLabelRules = !projectLabelRuleRepository.findByProject_ProjectId(projectId).isEmpty();
        boolean hasPolicies = !projectPolicyRepository.findByProject(project).isEmpty();
        
        if (!hasLabelRules) {
            throw new RuntimeException("Vui lòng thêm quy tắc gắn nhãn cho dự án này trước khi giao nhiệm vụ.");
        }
        if (!hasPolicies) {
            throw new RuntimeException("Vui lòng thêm chính sách cho dự án này trước khi giao nhiệm vụ.");
        }

        // Kiểm tra dataset thuộc project
        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new RuntimeException("Bộ dữ liệu không được tìm thấy: " + request.getDatasetId()));

        if (!dataset.getProject().getProjectId().equals(projectId)) {
            throw new RuntimeException("Bộ dữ liệu không thuộc dự án này");
        }

        // Kiểm tra annotator
        User annotator = userRepository.findById(request.getAnnotatorId())
                .orElseThrow(() -> new RuntimeException("Người chú thích không được tìm thấy: " + request.getAnnotatorId()));

        if (!"ANNOTATOR".equalsIgnoreCase(annotator.getRole().getRoleName())) {
            throw new RuntimeException("Người dùng " + annotator.getUsername() + " không phải là ANNOTATOR");
        }

        // Kiểm tra reviewer
        User reviewer = userRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new RuntimeException("Người xem xét không được tìm thấy: " + request.getReviewerId()));

        if (!"REVIEWER".equalsIgnoreCase(reviewer.getRole().getRoleName())) {
            throw new RuntimeException("Người dùng " + reviewer.getUsername() + " không phải là REVIEWER");
        }

        // Kiểm tra dataset đã được assign chưa
        if (assignmentRepository.existsByDataset_DatasetIdAndAnnotator_UserId(
                request.getDatasetId(), request.getAnnotatorId())) {
            throw new RuntimeException("Bộ dữ liệu này đã được gán cho người chú thích này");
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

    /**
     * Manager xem danh sách phân công trong project
     */
    public List<AssignmentResponse> getAssignmentsByProject(Long projectId, Long managerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không được tìm thấy: " + projectId));

        if (!project.getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("Bạn không phải là quản lý của dự án này");
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
                .orElseThrow(() -> new RuntimeException("Phân công không được tìm thấy: " + assignmentId));

        if (!assignment.getProject().getManager().getUserId().equals(managerId)) {
            throw new RuntimeException("Bạn không phải là quản lý của dự án này");
        }

        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new RuntimeException("Không thể xóa phân công với trạng thái: " + assignment.getStatus());
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
                .displayStatus(getDisplayStatus(a.getStatus()))  // ✅ Map to display status
                .progress(a.getProgress())
                .completedAt(a.getCompletedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }

    /**
     * Convert status gốc → display status thân thiện
     * Logic: 3 trạng thái chính chỉ
     * - "Chờ xử lý" = PENDING
     * - "Đang xử lý" = IN_PROGRESS, SUBMITTED, RE_SUBMITTED, REJECTED
     * - "Hoàn thành" = APPROVED, COMPLETED
     */
    private String getDisplayStatus(AssignmentStatus status) {
        if (status == null) {
            return "Chờ xử lý";
        }
        
        switch (status) {
            case PENDING:
                return "Chờ xử lý";
            case IN_PROGRESS:
            case SUBMITTED:
            case RE_SUBMITTED:
            case REJECTED:
                return "Đang xử lý";
            case APPROVED:
            case COMPLETED:
                return "Hoàn thành";
            default:
                return "Chờ xử lý";
        }
    }
}

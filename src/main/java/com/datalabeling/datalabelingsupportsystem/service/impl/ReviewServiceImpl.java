package com.datalabeling.datalabelingsupportsystem.service.impl;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.ReviewAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.DataItem.DataItemResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelGroupResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotationResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotatorAssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.WorkSpace.AnnotationWorkspaceResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;
import com.datalabeling.datalabelingsupportsystem.enums.Reviewing.ReviewingStatus;
import com.datalabeling.datalabelingsupportsystem.exception.ResourceNotFoundException;
import com.datalabeling.datalabelingsupportsystem.exception.ValidationException;
import com.datalabeling.datalabelingsupportsystem.pojo.*;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.AssignmentRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DataItemRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Labeling.ReviewingRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.PolicyRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Users.UserRepository;
import com.datalabeling.datalabelingsupportsystem.service.Labeling.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

        private final AssignmentRepository assignmentRepository;
        private final ReviewingRepository reviewingRepository;
        private final DataItemRepository dataItemRepository;
        private final DatasetRepository datasetRepository;
        private final LabelRepository labelRepository;
        private final LabelRuleRepository labelRuleRepository;
        private final UserRepository userRepository;
        private final PolicyRepository policyRepository;
        private final ProjectRepository projectRepository;

        @Override
        public List<AnnotatorAssignmentResponse> getMyReviewAssignments(Long reviewerId) {
                List<Assignment> assignments = assignmentRepository.findByReviewer_UserId(reviewerId);
                return assignments.stream()
                                .map(a -> AnnotatorAssignmentResponse.builder()
                                                .assignmentId(a.getAssignmentId())
                                                .projectName(a.getProject().getName())
                                                .datasetName(a.getDataset().getName())
                                                .dataType(a.getProject().getDataType())
                                                .status(a.getStatus().name())
                                                .projectStatus(a.getProject().getStatus())
                                                .progress(a.getProgress())
                                                .completedAt(a.getCompletedAt())
                                                .annotatorName(a.getAnnotator().getFullName())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public AnnotationWorkspaceResponse openReviewWorkspace(Long assignmentId, Long reviewerId) {
                Assignment assignment = assignmentRepository.findById(assignmentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

                // Kiểm tra reviewer có được giao duyệt assignment này không
                if (assignment.getReviewer() == null || !assignment.getReviewer().getUserId().equals(reviewerId)) {
                        throw new ValidationException("Access denied: not assigned reviewer");
                }

                // Assignment phải ở trạng thái SUBMITTED, RE_SUBMITTED, REJECTED hoặc APPROVED để review
                if (!(assignment.getStatus() == AssignmentStatus.SUBMITTED
                                || assignment.getStatus() == AssignmentStatus.RE_SUBMITTED
                                || assignment.getStatus() == AssignmentStatus.REJECTED
                                || assignment.getStatus() == AssignmentStatus.APPROVED)) {
                        throw new ValidationException("Assignment is not ready for review");
                }

                // Lấy danh sách items và annotations
                List<DataItem> items = dataItemRepository
                                .findByDataset_DatasetId(assignment.getDataset().getDatasetId());

                List<String> guideUrls = labelRepository
                                .findGuideUrlsByDatasetId(assignment.getDataset().getDatasetId());

                List<DataItemResponse> itemResponses = items.stream()
                                .map(item -> {
                                        List<AnnotationResponse> annotations = reviewingRepository
                                                        .findByAssignment_AssignmentIdAndDataItem_ItemId(
                                                                        assignment.getAssignmentId(), item.getItemId())
                                                        .stream()
                                                        .map(this::toAnnotationResponse)
                                                        .collect(Collectors.toList());

                                        return DataItemResponse.builder()
                                                        .itemId(item.getItemId())
                                                        .fileUrl(item.getFileUrl())
                                                        .width(item.getWidth())
                                                        .height(item.getHeight())
                                                        .annotations(annotations)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // Lấy labels gom nhóm (by project)
                List<LabelRule> labelRules = labelRuleRepository
                                .findLabelRulesByProjectId(assignment.getProject().getProjectId());

                List<LabelGroupResponse> labelGroups = labelRules.stream()
                                .map(rule -> LabelGroupResponse.builder()
                                                .ruleId(rule.getRuleId())
                                                .ruleName(rule.getName())
                                                .labels(rule.getLabels().stream()
                                                                .filter(Label::getIsActive)
                                                                .map(l -> LabelResponse.builder()
                                                                                .labelId(l.getLabelId())
                                                                                .labelName(l.getLabelName())
                                                                                .colorCode(l.getColorCode())
                                                                                .labelType(l.getLabelType())
                                                                                .description(l.getDescription())
                                                                                .shortcutKey(l.getShortcutKey())
                                                                                .isActive(l.getIsActive())
                                                                                .build())
                                                                .collect(Collectors.toList()))
                                                .build())
                                .collect(Collectors.toList());

                return AnnotationWorkspaceResponse.builder()
                                .assignmentId(assignmentId)
                                .projectId(assignment.getProject().getProjectId())
                                .projectName(assignment.getProject().getName())
                                .projectGuidelineContent(assignment.getProject().getGuidelineContent())
                                .projectGuidelineVersion(assignment.getProject().getGuidelineVersion())
                                .projectGuidelineFileUrl(assignment.getProject().getGuidelineFileUrl())
                                .dataType(assignment.getProject().getDataType())
                                .items(itemResponses)
                                .labelGroups(labelGroups)
                                .labelGuideUrls(guideUrls)
                                .progress(assignment.getProgress() != null ? assignment.getProgress() : 0)
                                .assignmentStatus(assignment.getStatus().name())
                                // Calculate summary stats
                                .totalShapes(itemResponses.stream()
                                                .mapToInt(item -> item.getAnnotations().size())
                                                .sum())
                                .totalLabels((int) itemResponses.stream()
                                                .flatMap(item -> item.getAnnotations().stream())
                                                .map(AnnotationResponse::getLabelId)
                                                .distinct()
                                                .count())
                                .annotatedItems((int) itemResponses.stream()
                                                .filter(item -> !item.getAnnotations().isEmpty())
                                                .count())
                                .totalItems(itemResponses.size())
                                .build();
        }

        @Override
        public List<AnnotationResponse> getReviewAssignmentAnnotations(Long assignmentId, Long reviewerId) {
                Assignment assignment = assignmentRepository.findById(assignmentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

                // Kiểm tra reviewer
                if (assignment.getReviewer() == null || !assignment.getReviewer().getUserId().equals(reviewerId)) {
                        throw new ValidationException("Access denied: not assigned reviewer");
                }

                return reviewingRepository.findByAssignment_AssignmentId(assignmentId)
                                .stream()
                                .map(this::toAnnotationResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public List<AnnotationResponse> getReviewAnnotationsByItem(Long assignmentId, Long itemId, Long reviewerId) {
                Assignment assignment = assignmentRepository.findById(assignmentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

                // Kiểm tra reviewer
                if (assignment.getReviewer() == null || !assignment.getReviewer().getUserId().equals(reviewerId)) {
                        throw new ValidationException("Access denied: not assigned reviewer");
                }

                return reviewingRepository
                                .findByAssignment_AssignmentIdAndDataItem_ItemId(assignmentId, itemId)
                                .stream()
                                .map(this::toAnnotationResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public AnnotationResponse reviewAnnotation(Long reviewingId, ReviewAnnotationRequest request, Long reviewerId) {
                Reviewing reviewing = reviewingRepository.findById(reviewingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found"));

                Assignment assignment = getAssignment(reviewerId, reviewing);

                // cập nhật trạng thái và policy
                if (Boolean.TRUE.equals(request.getHasError())) {
                        if (request.getPolicyId() == null) {
                                throw new ValidationException("policyId is required when hasError is true");
                        }
                        Policy policy = policyRepository.findById(request.getPolicyId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
                        reviewing.setPolicy(policy);
                        reviewing.setStatus(ReviewingStatus.REJECTED);
                        reviewing.setIsImproved(false);  // ✅ Reset isImproved khi reject

                        // 🔴 IMPORTANT: Cập nhật assignment status ngay lập tức khi có reject
                        // Để annotator biết ngay có lỗi cần sửa, không phải chờ reviewer submit
                        if (assignment.getStatus() != AssignmentStatus.REJECTED) {
                                assignment.setStatus(AssignmentStatus.REJECTED);
                                assignmentRepository.save(assignment);
                        }
                } else {
                        // không có lỗi: approve
                        reviewing.setPolicy(null);
                        reviewing.setStatus(ReviewingStatus.APPROVED);
                }

                // gán reviewer và lưu
                User reviewer = userRepository.findById(reviewerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));
                reviewing.setReviewer(reviewer);
                reviewing.setNote(request.getNote());  // ✅ Lưu ghi chú của reviewer
                reviewing = reviewingRepository.save(reviewing);
                return toAnnotationResponse(reviewing);
        }

        @Override
        @Transactional
        public void submitReview(Long assignmentId, Long reviewerId) {
                Assignment assignment = assignmentRepository.findById(assignmentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

                if (assignment.getReviewer() == null || !assignment.getReviewer().getUserId().equals(reviewerId)) {
                        throw new ValidationException("Access denied: not assigned reviewer");
                }

                if (assignment.getStatus() != AssignmentStatus.SUBMITTED
                                && assignment.getStatus() != AssignmentStatus.RE_SUBMITTED
                                && assignment.getStatus() != AssignmentStatus.REJECTED
                                && assignment.getStatus() != AssignmentStatus.APPROVED) {
                        throw new ValidationException("Assignment is not in a reviewable state. Current status: "
                                        + assignment.getStatus());
                }

                List<Reviewing> allReviewings = reviewingRepository.findByAssignment_AssignmentId(assignmentId);
                if (allReviewings.isEmpty()) {
                        throw new ValidationException("Không có annotation nào để đánh giá");
                }

                long pendingCount = allReviewings.stream()
                                .filter(r -> r.getStatus() == ReviewingStatus.PENDING || r.getStatus() == null)
                                .count();
                if (pendingCount > 0) {
                        throw new ValidationException(
                                        "Còn " + pendingCount
                                                        + " annotation chưa được đánh giá. Vui lòng xét hết trước khi nộp.");
                }

                // Nếu có annotation bị REJECTED → gán trạng thái REJECTED để annotator sửa lại
                long rejectedCount = allReviewings.stream()
                                .filter(r -> r.getStatus() == ReviewingStatus.REJECTED)
                                .count();

                assignment.setStatus(rejectedCount > 0 ? AssignmentStatus.REJECTED : AssignmentStatus.APPROVED);
                assignmentRepository.save(assignment);
                if (rejectedCount == 0) {
                        syncProjectStatusAfterReview(assignment.getProject());
                }
        }

        private void syncProjectStatusAfterReview(Project project) {
                if (project == null) {
                        return;
                }

                List<Dataset> projectDatasets = datasetRepository.findByProject_ProjectId(project.getProjectId());
                if (projectDatasets.isEmpty()) {
                        return;
                }

                List<Assignment> projectAssignments = assignmentRepository
                                .findByProject_ProjectId(project.getProjectId());
                if (projectAssignments.isEmpty()) {
                        if (!"IN_PROGRESS".equals(project.getStatus())) {
                                project.setStatus("IN_PROGRESS");
                                projectRepository.save(project);
                        }
                        return;
                }

                boolean allBatchesApproved = projectDatasets.stream().allMatch(dataset -> {
                        List<Assignment> datasetAssignments = projectAssignments.stream()
                                        .filter(a -> a.getDataset() != null
                                                        && dataset.getDatasetId().equals(a.getDataset().getDatasetId()))
                                        .toList();

                        // Batch chưa được phân công thì chưa thể hoàn thành project.
                        if (datasetAssignments.isEmpty()) {
                                return false;
                        }

                        return datasetAssignments.stream()
                                        .allMatch(a -> a.getStatus() == AssignmentStatus.APPROVED);
                });

                String targetStatus = allBatchesApproved ? "COMPLETED" : "IN_PROGRESS";

                if (!targetStatus.equals(project.getStatus())) {
                        project.setStatus(targetStatus);
                        projectRepository.save(project);
                }
        }

        private static Assignment getAssignment(Long reviewerId, Reviewing reviewing) {
                Assignment assignment = reviewing.getAssignment();
                // đảm bảo reviewer chính là người được phân công
                if (assignment.getReviewer() == null || !assignment.getReviewer().getUserId().equals(reviewerId)) {
                        throw new ValidationException("Access denied: only assigned reviewer can review");
                }

                // assignment phải đang ở trạng thái SUBMITTED, RE_SUBMITTED, REJECTED hoặc APPROVED để review
                if (!(assignment.getStatus() == AssignmentStatus.SUBMITTED
                                || assignment.getStatus() == AssignmentStatus.RE_SUBMITTED
                                || assignment.getStatus() == AssignmentStatus.REJECTED
                                || assignment.getStatus() == AssignmentStatus.APPROVED)) {
                        throw new ValidationException("Assignment is not ready for review");
                }
                return assignment;
        }

        private AnnotationResponse toAnnotationResponse(Reviewing r) {
                return AnnotationResponse.builder()
                                .reviewingId(r.getReviewingId())
                                .itemId(r.getDataItem().getItemId())
                                .labelId(r.getLabel().getLabelId())
                                .labelName(r.getLabel().getLabelName())
                                .colorCode(r.getLabel().getColorCode())
                                .labelType(r.getLabel().getLabelType())
                                .geometry(r.getGeometry())
                                .status(r.getStatus())
                                .isImproved(r.getIsImproved())
                                .reviewerId(r.getReviewer() != null ? r.getReviewer().getUserId() : null)
                                .reviewerName(r.getReviewer() != null ? r.getReviewer().getFullName() : null)
                                .policyId(r.getPolicy() != null ? r.getPolicy().getPolicyId() : null)
                                .policyName(r.getPolicy() != null ? r.getPolicy().getErrorName() : null)
                                .note(r.getNote())
                                .build();
        }
}

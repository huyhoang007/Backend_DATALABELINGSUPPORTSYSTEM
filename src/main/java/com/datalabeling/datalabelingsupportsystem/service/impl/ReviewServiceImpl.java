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
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Labeling.ReviewingRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.PolicyRepository;
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
    private final LabelRuleRepository labelRuleRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;

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

        // Assignment phải ở trạng thái SUBMITTED hoặc REJECTED để review
        if (!(assignment.getStatus() == AssignmentStatus.SUBMITTED
                || assignment.getStatus() == AssignmentStatus.REJECTED)) {
            throw new ValidationException("Assignment is not ready for review");
        }

        // Lấy danh sách items và annotations
        List<DataItem> items = dataItemRepository
                .findByDataset_DatasetId(assignment.getDataset().getDatasetId());

        List<DataItemResponse> itemResponses = items.stream()
                .map(item -> {
                    List<AnnotationResponse> annotations = reviewingRepository
                            .findByAssignment_AssignmentIdAndDataItem_ItemId(assignment.getAssignmentId(), item.getItemId())
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
                .projectName(assignment.getProject().getName())
                .dataType(assignment.getProject().getDataType())
                .items(itemResponses)
                .labelGroups(labelGroups)
                .progress(assignment.getProgress() != null ? assignment.getProgress() : 0)
                .assignmentStatus(assignment.getStatus().name())
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

            // khi một annotation bị từ chối, assignment ngay lập tức bị REJECTED
            assignment.setStatus(AssignmentStatus.REJECTED);
            assignmentRepository.save(assignment);
        } else {
            // không có lỗi: approve
            reviewing.setPolicy(null);
            reviewing.setStatus(ReviewingStatus.APPROVED);

            // nếu tất cả annotation thuộc assignment đều không bị reject thì đánh dấu hoàn thành
            boolean anyRejected = reviewingRepository
                    .findByAssignment_AssignmentId(assignment.getAssignmentId())
                    .stream()
                    .anyMatch(r -> r.getStatus() == ReviewingStatus.REJECTED);
            if (!anyRejected) {
                assignment.setStatus(AssignmentStatus.APPROVED);
                assignmentRepository.save(assignment);
            }
        }

        // gán reviewer và lưu
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));
        reviewing.setReviewer(reviewer);
        reviewing = reviewingRepository.save(reviewing);
        return toAnnotationResponse(reviewing);
    }

    private static Assignment getAssignment(Long reviewerId, Reviewing reviewing) {
        Assignment assignment = reviewing.getAssignment();
        // đảm bảo reviewer chính là người được phân công
        if (assignment.getReviewer() == null || !assignment.getReviewer().getUserId().equals(reviewerId)) {
            throw new ValidationException("Access denied: only assigned reviewer can review");
        }

        // assignment phải đang ở trạng thái SUBMITTED hoặc REJECTED để review
        if (!(assignment.getStatus() == AssignmentStatus.SUBMITTED
                || assignment.getStatus() == AssignmentStatus.REJECTED)) {
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
                .build();
    }
}

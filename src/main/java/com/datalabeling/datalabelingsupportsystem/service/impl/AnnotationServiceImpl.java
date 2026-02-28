package com.datalabeling.datalabelingsupportsystem.service.impl;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.BatchSaveAnnotationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Labeling.ReviewingRepository;
import com.datalabeling.datalabelingsupportsystem.service.Labeling.AnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnotationServiceImpl implements AnnotationService {

        private final AssignmentRepository assignmentRepository;
        private final ReviewingRepository reviewingRepository;
        private final DataItemRepository dataItemRepository;
        private final LabelRepository labelRepository;
        private final LabelRuleRepository labelRuleRepository;
        private final ObjectMapper objectMapper;

        // 1. LẤY DANH SÁCH TASK CỦA ANNOTATOR
        @Override
        public List<AnnotatorAssignmentResponse> getMyAssignments(Long annotatorId) {
                return assignmentRepository.findByAnnotator_UserId(annotatorId)
                                .stream()
                                .map(a -> AnnotatorAssignmentResponse.builder()
                                                .assignmentId(a.getAssignmentId())
                                                .projectName(a.getProject().getName())
                                                .datasetName(a.getDataset().getName())
                                                .dataType(a.getProject().getDataType())
                                                .status(a.getStatus().name())
                                                .progress(a.getProgress())
                                                .completedAt(a.getCompletedAt())
                                                .reviewerName(a.getReviewer() != null
                                                                ? a.getReviewer().getFullName()
                                                                : null)
                                                .build())
                                .collect(Collectors.toList());
        }

        // 2. MỞ WORKSPACE GÁN NHÃN
        @Override
        @Transactional
        public AnnotationWorkspaceResponse openWorkspace(Long assignmentId, Long annotatorId) {

                Assignment assignment = assignmentRepository
                                .findByAssignmentIdAndAnnotator_UserId(assignmentId, annotatorId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Assignment not found or access denied"));

                if (assignment.getStatus() == AssignmentStatus.APPROVED) {
                        throw new ValidationException("Assignment is already approved");
                }

                if (assignment.getStatus() == AssignmentStatus.PENDING) {
                        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
                        assignmentRepository.save(assignment);
                }

                List<DataItem> items = dataItemRepository
                                .findByDataset_DatasetId(assignment.getDataset().getDatasetId());

                // Lấy guide URLs
                List<String> guideUrls = labelRepository
                                .findGuideUrlsByDatasetId(assignment.getDataset().getDatasetId());

                // Map items + annotations
                List<DataItemResponse> itemResponses = items.stream()
                                .map(item -> {
                                        List<AnnotationResponse> annotations = reviewingRepository
                                                        .findByAssignment_AssignmentIdAndDataItem_ItemId(
                                                                        assignmentId, item.getItemId())
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

                // ✅ Lấy labels gom nhóm theo LabelRule
                List<LabelRule> labelRules = labelRuleRepository
                                .findLabelRulesByDatasetId(assignment.getDataset().getDatasetId());

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
                                .labelGroups(labelGroups) // ✅ trả về theo nhóm
                                .labelGuideUrls(guideUrls)
                                .progress(assignment.getProgress() != null ? assignment.getProgress() : 0)
                                .assignmentStatus(assignment.getStatus().name())
                                .build();
        }

        // 3. LƯU ANNOTATIONS CHO 1 ITEM (1 hoặc nhiều label)
        // Dùng trong lúc gán nhãn bình thường: dán nhãn, sửa, xóa rồi dán lại
        @Override
        @Transactional
        public List<AnnotationResponse> saveAnnotations(Long assignmentId,
                        BatchSaveAnnotationRequest request, Long annotatorId) {

                Assignment assignment = assignmentRepository
                                .findByAssignmentIdAndAnnotator_UserId(assignmentId, annotatorId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Assignment not found or access denied"));

                // Chỉ cho phép khi đang gán nhãn (IN_PROGRESS)
                if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS
                                && assignment.getStatus() != AssignmentStatus.PENDING) {
                        throw new ValidationException(
                                        "Can only edit annotations when assignment is IN_PROGRESS. " +
                                        "If assignment is REJECTED, use the fix-rejected endpoint.");
                }

                if (assignment.getStatus() == AssignmentStatus.PENDING) {
                        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
                }

                DataItem item = dataItemRepository.findById(request.getItemId())
                                .orElseThrow(() -> new ResourceNotFoundException("DataItem not found"));

                // Xóa hết annotations cũ của item này
                reviewingRepository.deleteAll(
                                reviewingRepository.findByAssignment_AssignmentIdAndDataItem_ItemId(
                                                assignmentId, item.getItemId()));

                // Lưu annotations mới (isImproved = false vì đây là gán nhãn mới)
                List<Reviewing> newReviews = buildReviews(request, assignment, item, false);
                newReviews = reviewingRepository.saveAll(newReviews);
                updateProgress(assignment);

                return newReviews.stream().map(this::toAnnotationResponse).collect(Collectors.toList());
        }

        // 3b. SỬA LẠI ANNOTATIONS SAU KHI REVIEWER REJECT
        // Chỉ hoạt động khi assignment đang ở trạng thái REJECTED
        // Đánh dấu isImproved = true để reviewer biết đã được sửa
        @Override
        @Transactional
        public List<AnnotationResponse> fixRejectedAnnotations(Long assignmentId,
                        BatchSaveAnnotationRequest request, Long annotatorId) {

                Assignment assignment = assignmentRepository
                                .findByAssignmentIdAndAnnotator_UserId(assignmentId, annotatorId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Assignment not found or access denied"));

                // Chỉ cho phép khi assignment bị REJECTED
                if (assignment.getStatus() != AssignmentStatus.REJECTED) {
                        throw new ValidationException(
                                        "This endpoint is only for fixing rejected assignments. " +
                                        "Current status: " + assignment.getStatus());
                }

                DataItem item = dataItemRepository.findById(request.getItemId())
                                .orElseThrow(() -> new ResourceNotFoundException("DataItem not found"));

                // Xóa hết annotations cũ của item này (cả REJECTED lẫn APPROVED của item đó)
                reviewingRepository.deleteAll(
                                reviewingRepository.findByAssignment_AssignmentIdAndDataItem_ItemId(
                                                assignmentId, item.getItemId()));

                // Lưu annotations mới với isImproved = true
                List<Reviewing> newReviews = buildReviews(request, assignment, item, true);
                newReviews = reviewingRepository.saveAll(newReviews);

                // Chuyển assignment → IN_PROGRESS để annotator có thể tiếp tục sửa các item khác
                assignment.setStatus(AssignmentStatus.IN_PROGRESS);
                assignmentRepository.save(assignment);

                updateProgress(assignment);

                return newReviews.stream().map(this::toAnnotationResponse).collect(Collectors.toList());
        }

        // 4. LẤY ANNOTATIONS THEO ITEM
        @Override
        public List<AnnotationResponse> getAnnotationsByItem(Long assignmentId,
                        Long itemId, Long annotatorId) {

                assignmentRepository
                                .findByAssignmentIdAndAnnotator_UserId(assignmentId, annotatorId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Assignment not found or access denied"));

                return reviewingRepository
                                .findByAssignment_AssignmentIdAndDataItem_ItemId(assignmentId, itemId)
                                .stream()
                                .map(this::toAnnotationResponse)
                                .collect(Collectors.toList());
        }

        // 7. NỘP ASSIGNMENT ĐỂ REVIEWER ĐÁNH GIÁ
        @Override
        @Transactional
        public void submitAssignment(Long assignmentId, Long annotatorId) {
                Assignment assignment = assignmentRepository
                                .findByAssignmentIdAndAnnotator_UserId(assignmentId, annotatorId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Assignment not found or access denied"));

                switch (assignment.getStatus()) {
                        case SUBMITTED ->
                                throw new ValidationException("Assignment is already submitted");
                        case APPROVED ->
                                throw new ValidationException("Assignment is already approved");
                        case PENDING ->
                                throw new ValidationException(
                                                "Please start annotating before submitting");
                        default -> {
                                // IN_PROGRESS hoặc REJECTED → cho phép nộp
                        }
                }

                // Kiểm tra có ít nhất 1 annotation chưa
                long totalAnnotations = reviewingRepository
                                .countByAssignment_AssignmentId(assignmentId);
                if (totalAnnotations == 0) {
                        throw new ValidationException(
                                        "Please add at least one annotation before submitting");
                }

                assignment.setStatus(AssignmentStatus.SUBMITTED);
                assignment.setCompletedAt(LocalDateTime.now());
                assignmentRepository.save(assignment);
        }

        /**
         * Kiểm tra assignment có cho phép chỉnh sửa annotation không
         * PENDING, IN_PROGRESS → được phép
         * SUBMITTED, APPROVED, REJECTED → không được phép qua endpoint này
         */
        private void checkCanEdit(Assignment assignment) {
                if (assignment.getStatus() == AssignmentStatus.SUBMITTED) {
                        throw new ValidationException(
                                        "Cannot edit: assignment is waiting for review");
                }
                if (assignment.getStatus() == AssignmentStatus.APPROVED) {
                        throw new ValidationException(
                                        "Cannot edit: assignment is already approved");
                }
        }

        /**
         * Tạo list Reviewing từ request
         * isImproved = false: gán nhãn mới bình thường
         * isImproved = true: sửa lại sau khi bị reviewer reject
         */
        private List<Reviewing> buildReviews(BatchSaveAnnotationRequest request,
                        Assignment assignment, DataItem item, boolean isImproved) {
                return request.getAnnotations().stream()
                                .map(ann -> {
                                        Label label = labelRepository.findById(ann.getLabelId())
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                        "Label not found: " + ann.getLabelId()));
                                        return Reviewing.builder()
                                                        .assignment(assignment)
                                                        .annotator(assignment.getAnnotator())
                                                        .dataItem(item)
                                                        .label(label)
                                                        .geometry(convertGeometry(ann.getGeometry()))
                                                        .status(ReviewingStatus.PENDING)
                                                        .isImproved(isImproved)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Tính và cập nhật progress của assignment
         * Progress = (số item đã có annotation / tổng số item) * 100
         */
        private void updateProgress(Assignment assignment) {
                long totalItems = dataItemRepository
                                .countByDataset_DatasetIdAndIsActiveTrue(assignment.getDataset().getDatasetId());
                if (totalItems == 0)
                        return;

                long annotatedItems = reviewingRepository
                                .countAnnotatedItems(assignment.getAssignmentId());
                int progress = (int) ((annotatedItems * 100) / totalItems);
                assignment.setProgress(progress);
                assignmentRepository.save(assignment);
        }

        /**
         * Convert JsonNode (object từ frontend) hoặc null → String để lưu DB
         */
        private String convertGeometry(JsonNode geometry) {
                if (geometry == null || geometry.isNull()) return null;
                try {
                        return objectMapper.writeValueAsString(geometry);
                } catch (Exception e) {
                        return geometry.toString();
                }
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

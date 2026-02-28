package com.datalabeling.datalabelingsupportsystem.controller.Labeling;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.BatchSaveAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.UpdateAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.ReviewAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotationResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotatorAssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.WorkSpace.AnnotationWorkspaceResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.service.Labeling.AnnotationService;
import com.datalabeling.datalabelingsupportsystem.service.Labeling.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnnotationController {

    private final AnnotationService annotationService;
    private final ReviewService reviewService;

    /**
     * Annotator xem danh sách task được giao
     */
    @GetMapping("/my-assignments")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<List<AnnotatorAssignmentResponse>> getMyAssignments(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(annotationService.getMyAssignments(annotatorId));
    }

    /**
     * Mở workspace gán nhãn
     */
    @GetMapping("/assignments/{assignmentId}/workspace")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<AnnotationWorkspaceResponse> openWorkspace(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(annotationService.openWorkspace(assignmentId, annotatorId));
    }

    /**
     * Lưu annotations cho 1 ảnh — hỗ trợ cả 1 label lẫn nhiều label.
     * Mỗi lần gọi sẽ REPLACE toàn bộ annotations cũ của item đó.
     * Frontend chỉ cần gọi 1 endpoint này khi user hoàn thành gán nhãn xong 1 ảnh.
     */
    @PostMapping("/assignments/{assignmentId}/annotations")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<List<AnnotationResponse>> saveAnnotations(
            @PathVariable Long assignmentId,
            @Valid @RequestBody BatchSaveAnnotationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(annotationService.saveAnnotations(assignmentId, request, annotatorId));
    }

    /**
     * Cập nhật annotation
     */
    @PutMapping("/annotations/{reviewingId}")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<AnnotationResponse> updateAnnotation(
            @PathVariable Long reviewingId,
            @RequestBody UpdateAnnotationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(annotationService.updateAnnotation(reviewingId, request, annotatorId));
    }

    /**
     * Xóa annotation
     */
    @DeleteMapping("/annotations/{reviewingId}")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<Void> deleteAnnotation(
            @PathVariable Long reviewingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        annotationService.deleteAnnotation(reviewingId, annotatorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy annotations theo item
     */
    @GetMapping("/assignments/{assignmentId}/items/{itemId}/annotations")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<List<AnnotationResponse>> getAnnotationsByItem(
            @PathVariable Long assignmentId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(annotationService.getAnnotationsByItem(assignmentId, itemId, annotatorId));
    }

    /**
     * Nộp assignment để reviewer đánh giá
     */
    @PostMapping("/assignments/{assignmentId}/submit")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<Void> submitAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        annotationService.submitAssignment(assignmentId, annotatorId);
        return ResponseEntity.ok().build();
    }

    /**
     * Reviewer đánh giá annotation cụ thể
     */
    @PostMapping("/annotations/{reviewingId}/review")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<AnnotationResponse> reviewAnnotation(
            @PathVariable Long reviewingId,
            @Valid @RequestBody ReviewAnnotationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(reviewService.reviewAnnotation(reviewingId, request, reviewerId));
    }

    // ============== REVIEWER ENDPOINTS ==============

    /**
     * Reviewer xem danh sách assignments được giao duyệt
     */
    @GetMapping("/my-review-assignments")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<List<AnnotatorAssignmentResponse>> getMyReviewAssignments(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(reviewService.getMyReviewAssignments(reviewerId));
    }

    /**
     * Reviewer mở workspace duyệt assignment
     */
    @GetMapping("/assignments/{assignmentId}/review-workspace")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<AnnotationWorkspaceResponse> openReviewWorkspace(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(reviewService.openReviewWorkspace(assignmentId, reviewerId));
    }

    /**
     * Reviewer xem danh sách annotations của assignment
     */
    @GetMapping("/assignments/{assignmentId}/review-annotations")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<List<AnnotationResponse>> getReviewAssignmentAnnotations(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(reviewService.getReviewAssignmentAnnotations(assignmentId, reviewerId));
    }

    /**
     * Reviewer xem danh sách annotations theo item
     */
    @GetMapping("/assignments/{assignmentId}/items/{itemId}/review-annotations")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<List<AnnotationResponse>> getReviewAnnotationsByItem(
            @PathVariable Long assignmentId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = ((User) userDetails).getUserId();
        return ResponseEntity.ok(reviewService.getReviewAnnotationsByItem(assignmentId, itemId, reviewerId));
    }
}

package com.datalabeling.datalabelingsupportsystem.controller.Labeling;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.SaveAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.UpdateAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotationResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotatorAssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.WorkSpace.AnnotationWorkspaceResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import com.datalabeling.datalabelingsupportsystem.service.Labeling.AnnotationService;
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
     * Lưu annotation mới
     */
    @PostMapping("/assignments/{assignmentId}/annotations")
    @PreAuthorize("hasRole('ANNOTATOR')")
    public ResponseEntity<AnnotationResponse> saveAnnotation(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SaveAnnotationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long annotatorId = ((User) userDetails).getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(annotationService.saveAnnotation(assignmentId, request, annotatorId));
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
}

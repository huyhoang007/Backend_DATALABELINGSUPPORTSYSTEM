package com.datalabeling.datalabelingsupportsystem.service.Labeling;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.ReviewAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotationResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotatorAssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.WorkSpace.AnnotationWorkspaceResponse;

import java.util.List;

/**
 * Service for reviewer/reviewing operations.
 * Handles reviewing assignments, viewing annotations, and validating quality.
 */
public interface ReviewService {

    /**
     * Reviewer xem danh sách assignments được giao duyệt
     */
    List<AnnotatorAssignmentResponse> getMyReviewAssignments(Long reviewerId);

    /**
     * Reviewer mở workspace duyệt assignment (xem items + labels + annotations)
     */
    AnnotationWorkspaceResponse openReviewWorkspace(Long assignmentId, Long reviewerId);

    /**
     * Reviewer xem danh sách annotations của một assignment
     */
    List<AnnotationResponse> getReviewAssignmentAnnotations(Long assignmentId, Long reviewerId);

    /**
     * Reviewer xem danh sách annotations theo item (để duyệt chi tiết)
     */
    List<AnnotationResponse> getReviewAnnotationsByItem(Long assignmentId, Long itemId, Long reviewerId);

    /**
     * Reviewer đánh giá annotation cụ thể.
     * Nếu hasError=true → gắn policy vi phạm, đổi status thành REJECTED.
     * Nếu hasError=false → chấp nhận, status thành APPROVED.
     *
     * @param reviewingId id của reviewing
     * @param request     nội dung đánh giá (có lỗi hay không, policy id nếu lỗi)
     * @param reviewerId  id người review (lấy từ authentication principal)
     * @return annotation response với status mới
     */
    AnnotationResponse reviewAnnotation(Long reviewingId, ReviewAnnotationRequest request, Long reviewerId);
}

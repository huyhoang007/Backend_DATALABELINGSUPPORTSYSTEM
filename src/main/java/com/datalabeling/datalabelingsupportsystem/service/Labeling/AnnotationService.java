package com.datalabeling.datalabelingsupportsystem.service.Labeling;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.SaveAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.UpdateAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.ReviewAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotationResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotatorAssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.WorkSpace.AnnotationWorkspaceResponse;

import java.util.List;

public interface AnnotationService {

    // Lấy danh sách task của annotator
    List<AnnotatorAssignmentResponse> getMyAssignments(Long annotatorId);

    // Mở workspace gán nhãn
    AnnotationWorkspaceResponse openWorkspace(Long assignmentId, Long annotatorId);

    // Lưu 1 annotation
    AnnotationResponse saveAnnotation(Long assignmentId, SaveAnnotationRequest request, Long annotatorId);

    // Cập nhật annotation
    AnnotationResponse updateAnnotation(Long reviewingId, UpdateAnnotationRequest request, Long annotatorId);

    // Xóa annotation
    void deleteAnnotation(Long reviewingId, Long annotatorId);

    // Lấy annotations của 1 item trong assignment
    List<AnnotationResponse> getAnnotationsByItem(Long assignmentId, Long itemId, Long annotatorId);

    // Nộp để review
    void submitAssignment(Long assignmentId, Long annotatorId);

    /**
     * Reviewer đánh giá annotation cụ thể.
     *
     * @param reviewingId id của reviewing
     * @param request     nội dung đánh giá (có lỗi hay không, policy id nếu lỗi)
     * @param reviewerId  id người review (lấy từ authentication principal)
     * @return trạng thái annotation sau khi review
     */
    AnnotationResponse reviewAnnotation(Long reviewingId, ReviewAnnotationRequest request, Long reviewerId);
}

package com.datalabeling.datalabelingsupportsystem.service.Labeling;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.BatchSaveAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotationResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Labeling.AnnotatorAssignmentResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.WorkSpace.AnnotationWorkspaceResponse;

import java.util.List;

public interface AnnotationService {

    // Lấy danh sách task của annotator
    List<AnnotatorAssignmentResponse> getMyAssignments(Long annotatorId);

    // Mở workspace gán nhãn
    AnnotationWorkspaceResponse openWorkspace(Long assignmentId, Long annotatorId);

    // Lưu annotations cho 1 item (1 hoặc nhiều label đều được)
    // Luôn replace toàn bộ annotations cũ của item đó
    // Dùng cho cả save, update, và xóa (gửi list rỗng = xóa hết)
    List<AnnotationResponse> saveAnnotations(Long assignmentId, BatchSaveAnnotationRequest request, Long annotatorId);

    // Sửa lại annotations sau khi reviewer reject
    // Khác saveAnnotations: chỉ cho phép khi REJECTED + đánh dấu isImproved = true
    List<AnnotationResponse> fixRejectedAnnotations(Long assignmentId, BatchSaveAnnotationRequest request, Long annotatorId);

    // Lấy annotations của 1 item trong assignment
    List<AnnotationResponse> getAnnotationsByItem(Long assignmentId, Long itemId, Long annotatorId);

    // Nộp để review
    void submitAssignment(Long assignmentId, Long annotatorId);
}

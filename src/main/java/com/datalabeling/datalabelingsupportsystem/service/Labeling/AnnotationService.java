package com.datalabeling.datalabelingsupportsystem.service.Labeling;

import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.BatchSaveAnnotationRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Labeling.UpdateAnnotationRequest;
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
    List<AnnotationResponse> saveAnnotations(Long assignmentId, BatchSaveAnnotationRequest request, Long annotatorId);

    // Cập nhật 1 annotation cụ thể (sửa geometry hoặc label)
    AnnotationResponse updateAnnotation(Long reviewingId, UpdateAnnotationRequest request, Long annotatorId);

    // Xóa annotation
    void deleteAnnotation(Long reviewingId, Long annotatorId);

    // Lấy annotations của 1 item trong assignment
    List<AnnotationResponse> getAnnotationsByItem(Long assignmentId, Long itemId, Long annotatorId);

    // Nộp để review
    void submitAssignment(Long assignmentId, Long annotatorId);
}

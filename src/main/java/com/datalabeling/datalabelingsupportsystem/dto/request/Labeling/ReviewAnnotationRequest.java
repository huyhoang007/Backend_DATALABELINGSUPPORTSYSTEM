package com.datalabeling.datalabelingsupportsystem.dto.request.Labeling;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hoạt động của reviewer khi kiểm duyệt annotation.
 * <p>
 * - hasError = false: đánh dấu annotation hợp lệ, trạng thái APPROVED.
 * - hasError = true: phải cung cấp policyId (lỗi vi phạm), trạng thái REJECTED.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAnnotationRequest {
    @NotNull
    private Boolean hasError;

    // chỉ bắt buộc khi hasError == true
    private Long policyId;
}
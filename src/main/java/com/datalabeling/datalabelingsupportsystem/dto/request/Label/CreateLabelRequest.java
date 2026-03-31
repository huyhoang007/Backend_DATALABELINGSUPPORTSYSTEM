package com.datalabeling.datalabelingsupportsystem.dto.request.Label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLabelRequest {

    @NotBlank(message = "Tên nhãn là bắt buộc")
    private String labelName;

    @NotBlank(message = "Mã màu là bắt buộc")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Định dạng mã màu không hợp lệ")
    private String colorCode;

    @NotBlank(message = "Loại nhãn là bắt buộc")
    private String labelType;

    private String description;

    @Size(max = 20, message = "Phím tắt không được vượt quá 20 ký tự")
    private String shortcutKey;
}
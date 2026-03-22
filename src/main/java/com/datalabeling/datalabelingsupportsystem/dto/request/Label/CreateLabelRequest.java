package com.datalabeling.datalabelingsupportsystem.dto.request.Label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLabelRequest {

    @NotBlank(message = "Label name is required")
    private String labelName;

    @NotBlank(message = "Color code is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Invalid color code format")
    private String colorCode;

    @NotBlank(message = "Label type is required")
    private String labelType;

    private String description;

    @Size(max = 20, message = "Hotkey must not exceed 20 characters")
    private String shortcutKey;
}
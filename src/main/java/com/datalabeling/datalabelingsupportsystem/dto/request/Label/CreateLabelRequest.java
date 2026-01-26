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

    @Size(min = 1, max = 1, message = "Hotkey must be a single character")
    private String shortcutKey;
}
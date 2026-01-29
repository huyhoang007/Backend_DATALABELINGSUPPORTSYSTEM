package com.datalabeling.datalabelingsupportsystem.dto.request.Label;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AttachLabelsRequest {
    @NotEmpty
    private Set<Long> labelIds;
}

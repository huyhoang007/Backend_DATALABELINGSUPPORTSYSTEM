package com.datalabeling.datalabelingsupportsystem.dto.response.WorkSpace;

import com.datalabeling.datalabelingsupportsystem.dto.response.DataItem.DataItemResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelGroupResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnnotationWorkspaceResponse {
    private Long assignmentId;
    private String projectName;
    private String dataType;
    private List<DataItemResponse> items;
    private List<LabelGroupResponse> labelGroups;
    private List<String> labelGuideUrls;
    private Integer progress;
    private String assignmentStatus;
}

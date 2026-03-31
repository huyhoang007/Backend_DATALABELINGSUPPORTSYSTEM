package com.datalabeling.datalabelingsupportsystem.repository.Assignment.projection;

import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;

public interface ProjectAssignmentStatusCountProjection {
    Long getProjectId();
    AssignmentStatus getStatus();
    Long getTotal();
}

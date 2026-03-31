package com.datalabeling.datalabelingsupportsystem.repository.Assignment.projection;

import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;

public interface DatasetAssignmentStatusCountProjection {
    Long getDatasetId();
    AssignmentStatus getStatus();
    Long getTotal();
}

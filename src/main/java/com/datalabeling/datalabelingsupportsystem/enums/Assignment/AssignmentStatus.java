package com.datalabeling.datalabelingsupportsystem.enums.Assignment;

public enum AssignmentStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SUBMITTED,
    RE_SUBMITTED,   // Annotator đã sửa và nộp lại sau khi bị REJECTED
    APPROVED,
    REJECTED
}

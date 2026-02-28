package com.datalabeling.datalabelingsupportsystem.repository.Assignment;

import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;
import com.datalabeling.datalabelingsupportsystem.pojo.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByProject_ProjectId(Long projectId);

    List<Assignment> findByAnnotator_UserId(Long annotatorId);

    List<Assignment> findByAnnotator_UserIdAndStatus(Long annotatorId, AssignmentStatus status);

    Optional<Assignment> findByAssignmentIdAndAnnotator_UserId(Long assignmentId, Long annotatorId);

    // Reviewer queries
    List<Assignment> findByReviewer_UserId(Long reviewerId);

    boolean existsByDataset_DatasetIdAndAnnotator_UserId(Long datasetId, Long annotatorId);
}

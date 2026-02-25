package com.datalabeling.datalabelingsupportsystem.repository.Assignment;

import com.datalabeling.datalabelingsupportsystem.pojo.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByProject_ProjectId(Long projectId);

    List<Assignment> findByDataset_DatasetId(Long datasetId);

    List<Assignment> findByAnnotator_UserId(Long annotatorId);

    List<Assignment> findByReviewer_UserId(Long reviewerId);

    boolean existsByDataset_DatasetIdAndAnnotator_UserId(Long datasetId, Long annotatorId);
}

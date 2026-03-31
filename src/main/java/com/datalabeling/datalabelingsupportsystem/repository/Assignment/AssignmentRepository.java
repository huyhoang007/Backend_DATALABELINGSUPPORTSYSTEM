package com.datalabeling.datalabelingsupportsystem.repository.Assignment;

import com.datalabeling.datalabelingsupportsystem.enums.Assignment.AssignmentStatus;
import com.datalabeling.datalabelingsupportsystem.pojo.Assignment;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.projection.DatasetAssignmentStatusCountProjection;
import com.datalabeling.datalabelingsupportsystem.repository.Assignment.projection.ProjectAssignmentStatusCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select a.project.projectId as projectId, a.status as status, count(a) as total
            from Assignment a
            where a.project.projectId in :projectIds
            group by a.project.projectId, a.status
            """)
    List<ProjectAssignmentStatusCountProjection> countStatusesByProjectIds(@Param("projectIds") List<Long> projectIds);

    @Query("""
            select a.dataset.datasetId as datasetId, a.status as status, count(a) as total
            from Assignment a
            where a.project.projectId = :projectId
            group by a.dataset.datasetId, a.status
            """)
    List<DatasetAssignmentStatusCountProjection> countStatusesByProjectId(@Param("projectId") Long projectId);
}

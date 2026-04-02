package com.datalabeling.datalabelingsupportsystem.repository.Policy;

import com.datalabeling.datalabelingsupportsystem.pojo.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    long countByProject_ProjectId(Long projectId);

    long countByProject_ProjectIdAndAssignment_AssignmentId(Long projectId, Long assignmentId);

    long countByProject_ProjectIdAndAnnotator_UserId(Long projectId, Long userId);

    long countByProject_ProjectIdAndReviewer_UserId(Long projectId, Long userId);

    Violation findByReviewing_ReviewingIdAndPolicy_PolicyId(Long reviewingId, Long policyId);

    @Query("SELECT COUNT(DISTINCT v.reviewing.reviewingId) FROM Violation v WHERE v.project.projectId = :projectId")
    long countDistinctReviewingViolationsByProject(@Param("projectId") Long projectId);

    long countByProject_ProjectIdAndSeverity(Long projectId, Integer severity);

    long countByProject_ProjectIdAndViolationType(Long projectId, com.datalabeling.datalabelingsupportsystem.enums.Policies.ViolationType violationType);

    @Query("SELECT v.annotator.userId, COUNT(v) FROM Violation v WHERE v.project.projectId = :projectId GROUP BY v.annotator.userId")
    List<Object[]> countByProject_ProjectIdGroupByAnnotator(@Param("projectId") Long projectId);

}

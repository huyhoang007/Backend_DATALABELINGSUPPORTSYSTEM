package com.datalabeling.datalabelingsupportsystem.repository.Analytics;

import com.datalabeling.datalabelingsupportsystem.pojo.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Assignment, Long> {
    
    // Project Progress Queries
    @Query("SELECT COUNT(di) FROM DataItem di WHERE di.dataset.project.projectId = :projectId")
    long countTotalItemsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(DISTINCT r) FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId " +
            "AND r.status IN ('APPROVED', 'REJECTED')")
    long countLabeledItemsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(DISTINCT r) FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId " +
            "AND r.status = 'APPROVED'")
    long countApprovedItemsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(a) FROM Assignment a " +
            "WHERE a.project.projectId = :projectId " +
            "AND a.status = 'COMPLETED'")
    long countCompletedAssignmentsByProject(@Param("projectId") Long projectId);
    
    // Contribution Queries
    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.annotator.userId = :userId")
    long countTotalAssignmentsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(a) FROM Assignment a " +
            "WHERE a.annotator.userId = :userId AND a.status = 'COMPLETED'")
    long countCompletedAssignmentsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.annotator.userId = :userId " +
            "AND r.status IN ('APPROVED', 'REJECTED')")
    long countAnnotationsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.annotator.userId = :userId AND r.policy.policyId IS NOT NULL")
    long countPolicyViolationsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.reviewer.userId = :userId")
    long countReviewsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.reviewer.userId = :userId AND r.status = 'APPROVED'")
    long countApprovedReviewsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.reviewer.userId = :userId AND r.status = 'REJECTED'")
    long countRejectedReviewsByUser(@Param("userId") Long userId);
    
    // Quality Metrics Queries
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId")
    long countTotalAnnotationsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId AND r.status = 'APPROVED'")
    long countAcceptedAnnotationsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(DISTINCT r.policy) FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId " +
            "AND r.policy.policyId IS NOT NULL")
    long countPolicyViolationsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(DISTINCT r.label) FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId")
    long countDistinctLabelsUsed(@Param("projectId") Long projectId);
    
    @Query("SELECT r.label, COUNT(r) as count FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId " +
            "GROUP BY r.label ORDER BY count DESC")
    List<Object[]> getLabelDistributionByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(r) FROM Reviewing r " +
            "WHERE r.assignment.project.projectId = :projectId AND r.isImproved = true")
    long countImprovementsByProject(@Param("projectId") Long projectId);
    
    // User contribution for project
    @Query("SELECT DISTINCT a.annotator FROM Assignment a " +
            "WHERE a.project.projectId = :projectId")
    List<Object> findAnnotatorsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT DISTINCT a.reviewer FROM Assignment a " +
            "WHERE a.project.projectId = :projectId AND a.reviewer IS NOT NULL")
    List<Object> findReviewersByProject(@Param("projectId") Long projectId);
    
    // Use a native Postgres query to calculate average completion time in seconds.
    // The previous HQL implementation caused startup failures because Hibernate's
    // timestampdiff()/DATEDIFF() functions either require a temporal unit or are
    // treated as Objects.  A native query avoids the problem entirely.
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at))) " +
                   "FROM \"Assignments\" " +
                   "WHERE annotator_id = :userId AND completed_at IS NOT NULL",
           nativeQuery = true)
    Double getAverageCompletionTimeByUser(@Param("userId") Long userId);
    
}

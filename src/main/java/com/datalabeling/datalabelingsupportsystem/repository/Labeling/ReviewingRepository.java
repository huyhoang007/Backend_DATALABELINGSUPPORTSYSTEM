package com.datalabeling.datalabelingsupportsystem.repository.Labeling;

import com.datalabeling.datalabelingsupportsystem.pojo.Reviewing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewingRepository extends JpaRepository<Reviewing, Long> {

    List<Reviewing> findByAssignment_AssignmentIdAndDataItem_ItemId(
            Long assignmentId, Long itemId);

    List<Reviewing> findByAssignment_AssignmentId(Long assignmentId);

    @Query("SELECT COUNT(DISTINCT r.dataItem.itemId) FROM Reviewing r " +
            "WHERE r.assignment.assignmentId = :assignmentId " +
            "AND r.status IN ('APPROVED', 'PENDING')")
    long countAnnotatedItems(@Param("assignmentId") Long assignmentId);

    boolean existsByReviewingIdAndAnnotator_UserId(Long reviewingId, Long annotatorId);

    long countByAssignment_AssignmentId(Long assignmentAssignmentId);
}

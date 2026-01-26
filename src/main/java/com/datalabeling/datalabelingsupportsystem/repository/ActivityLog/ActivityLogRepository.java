package com.datalabeling.datalabelingsupportsystem.repository.ActivityLog;

import com.datalabeling.datalabelingsupportsystem.pojo.ActivityLog;
import com.datalabeling.datalabelingsupportsystem.pojo.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<ActivityLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<ActivityLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<ActivityLog> findByTargetOrderByCreatedAtDesc(String target, Pageable pageable);

    List<ActivityLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
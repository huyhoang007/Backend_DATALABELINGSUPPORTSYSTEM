package com.datalabeling.datalabelingsupportsystem.repository.Policy;

import com.datalabeling.datalabelingsupportsystem.enums.Policies.ErrorLevel;
import com.datalabeling.datalabelingsupportsystem.pojo.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Override
    Optional<Policy> findById(Long aLong);

    boolean existsByErrorName(String errorName);

    Page<Policy> findByErrorLevel(ErrorLevel errorLevel, Pageable pageable);

    Page<Policy> findAllByOrderByCreatedAtDesc(Pageable pageable);
}


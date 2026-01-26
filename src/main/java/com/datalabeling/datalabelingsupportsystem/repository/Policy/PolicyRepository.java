package com.datalabeling.datalabelingsupportsystem.repository.Policy;

import com.datalabeling.datalabelingsupportsystem.pojo.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByStatus(String status);

    boolean existsByName(String name);
}

package com.datalabeling.datalabelingsupportsystem.repository.Project;

import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByManagerUserId(Long managerId);

    List<Project> findByStatus(String status);

    boolean existsByNameAndManagerUserId(String name, Long managerId);
}
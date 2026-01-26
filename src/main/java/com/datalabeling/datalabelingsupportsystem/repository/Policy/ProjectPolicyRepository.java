package com.datalabeling.datalabelingsupportsystem.repository.Policy;

import com.datalabeling.datalabelingsupportsystem.pojo.Policy;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.pojo.ProjectPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPolicyRepository extends JpaRepository<ProjectPolicy, Long> {

    List<ProjectPolicy> findByProject(Project project);

    List<ProjectPolicy> findByPolicy(Policy policy);

    Optional<ProjectPolicy> findByProjectAndPolicy(Project project, Policy policy);

    boolean existsByProjectAndPolicy(Project project, Policy policy);

    void deleteByProjectAndPolicy(Project project, Policy policy);
}

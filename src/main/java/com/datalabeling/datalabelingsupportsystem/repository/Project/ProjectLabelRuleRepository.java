package com.datalabeling.datalabelingsupportsystem.repository.Project;

import com.datalabeling.datalabelingsupportsystem.pojo.ProjectLabelRule;
import com.datalabeling.datalabelingsupportsystem.pojo.ProjectLabelRuleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLabelRuleRepository extends JpaRepository<ProjectLabelRule, ProjectLabelRuleId> {

    List<ProjectLabelRule> findByProject_ProjectId(Long projectId);

    void deleteByProject_ProjectId(Long projectId);
}

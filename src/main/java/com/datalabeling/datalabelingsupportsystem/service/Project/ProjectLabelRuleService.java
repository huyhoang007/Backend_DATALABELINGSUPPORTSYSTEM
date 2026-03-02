package com.datalabeling.datalabelingsupportsystem.service.Project;

import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelRuleResponse;
import com.datalabeling.datalabelingsupportsystem.exception.ResourceNotFoundException;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRule;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.pojo.ProjectLabelRule;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectLabelRuleRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectLabelRuleService {

    private final ProjectLabelRuleRepository projectLabelRuleRepository;
    private final ProjectRepository projectRepository;
    private final LabelRuleRepository labelRuleRepository;

    /** Returns label rules currently linked to a project. */
    public List<LabelRuleResponse> getProjectLabelRules(Long projectId) {
        return projectLabelRuleRepository.findByProject_ProjectId(projectId)
                .stream()
                .map(plr -> mapToResponse(plr.getLabelRule()))
                .collect(Collectors.toList());
    }

    /** Replaces all label-rule links for a project atomically. */
    @Transactional
    public void setProjectLabelRules(Long projectId, List<Long> ruleIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        // Remove all existing links for this project
        projectLabelRuleRepository.deleteByProject_ProjectId(projectId);

        if (ruleIds == null || ruleIds.isEmpty()) {
            return;
        }

        // Resolve and save new links
        List<LabelRule> rules = labelRuleRepository.findAllById(ruleIds);
        List<ProjectLabelRule> links = rules.stream()
                .map(rule -> new ProjectLabelRule(project, rule))
                .collect(Collectors.toList());
        projectLabelRuleRepository.saveAll(links);
    }

    private LabelRuleResponse mapToResponse(LabelRule rule) {
        Set<LabelResponse> labels = rule.getLabels().stream()
                .map(l -> LabelResponse.builder()
                        .labelId(l.getLabelId())
                        .labelName(l.getLabelName())
                        .colorCode(l.getColorCode())
                        .labelType(l.getLabelType())
                        .description(l.getDescription())
                        .shortcutKey(l.getShortcutKey())
                        .isActive(l.getIsActive())
                        .build())
                .collect(Collectors.toSet());

        return LabelRuleResponse.builder()
                .ruleId(rule.getRuleId())
                .name(rule.getName())
                .ruleContent(rule.getRuleContent())
                .labels(labels)
                .build();
    }
}

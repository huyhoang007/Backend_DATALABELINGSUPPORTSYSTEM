package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_labelrule")
@Getter @Setter @NoArgsConstructor
public class ProjectLabelRule {

    @EmbeddedId
    private ProjectLabelRuleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ruleId")
    @JoinColumn(name = "rule_id", nullable = false)
    private LabelRule labelRule;

    public ProjectLabelRule(Project project, LabelRule labelRule) {
        this.project = project;
        this.labelRule = labelRule;
        this.id = new ProjectLabelRuleId(project.getProjectId(), labelRule.getRuleId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectLabelRule that = (ProjectLabelRule) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

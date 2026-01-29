package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "label_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String ruleContent;
    
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<LabelRuleLabel> labelLinks = new HashSet<>();

    // Convenience accessors to preserve previous API
    public Set<Label> getLabels() {
        return labelLinks.stream()
                .map(LabelRuleLabel::getLabel)
                .collect(Collectors.toSet());
    }

    public void addLabel(Label label) {
        LabelRuleLabel link = new LabelRuleLabel(this, label);
        if (!labelLinks.contains(link)) {
            labelLinks.add(link);
            label.getLabelLinks().add(link);
        }
    }

    public void removeLabel(Label label) {
        LabelRuleLabelId id = new LabelRuleLabelId(this.ruleId, label.getLabelId());
        labelLinks.removeIf(l -> l.getId().equals(id));
        label.getLabelLinks().removeIf(l -> l.getId().equals(id));
    }
}
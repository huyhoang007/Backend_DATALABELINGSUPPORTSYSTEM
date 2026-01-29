package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "labelrule_label")
@Getter
@Setter
@NoArgsConstructor
public class LabelRuleLabel {

    @EmbeddedId
    private LabelRuleLabelId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ruleId")
    @JoinColumn(name = "rule_id", nullable = false)
    private LabelRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("labelId")
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    public LabelRuleLabel(LabelRule rule, Label label) {
        this.rule = rule;
        this.label = label;
        Long rId = rule != null ? rule.getRuleId() : null;
        Long lId = label != null ? label.getLabelId() : null;
        this.id = new LabelRuleLabelId(rId, lId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelRuleLabel that = (LabelRuleLabel) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

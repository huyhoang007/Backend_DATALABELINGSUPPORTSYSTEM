package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dataset_labelrule")
@Getter @Setter @NoArgsConstructor
public class DatasetLabelRule {

    @EmbeddedId
    private DatasetLabelRuleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("datasetId")
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ruleId")
    @JoinColumn(name = "rule_id", nullable = false)
    private LabelRule labelRule;

    public DatasetLabelRule(Dataset dataset, LabelRule labelRule) {
        this.dataset = dataset;
        this.labelRule = labelRule;
        this.id = new DatasetLabelRuleId(dataset.getDatasetId(), labelRule.getRuleId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatasetLabelRule that = (DatasetLabelRule) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

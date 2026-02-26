package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DatasetLabelRuleId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "dataset_id")
    private Long datasetId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatasetLabelRuleId that = (DatasetLabelRuleId) o;
        return Objects.equals(datasetId, that.datasetId) &&
                Objects.equals(ruleId, that.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datasetId, ruleId);
    }
}

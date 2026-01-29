package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class LabelRuleLabelId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    private Long labelId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelRuleLabelId that = (LabelRuleLabelId) o;
        return Objects.equals(ruleId, that.ruleId) && Objects.equals(labelId, that.labelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, labelId);
    }
}

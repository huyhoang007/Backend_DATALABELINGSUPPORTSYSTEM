package com.datalabeling.datalabelingsupportsystem.repository.Label;

import com.datalabeling.datalabelingsupportsystem.pojo.LabelRuleLabel;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRuleLabelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRuleLabelRepository extends JpaRepository<LabelRuleLabel, LabelRuleLabelId> {
    List<LabelRuleLabel> findByIdRuleId(Long ruleId);
    List<LabelRuleLabel> findByIdLabelId(Long labelId);
}

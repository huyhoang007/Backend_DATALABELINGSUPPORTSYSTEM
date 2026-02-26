package com.datalabeling.datalabelingsupportsystem.repository.Label;

import com.datalabeling.datalabelingsupportsystem.pojo.LabelRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRuleRepository extends JpaRepository<LabelRule, Long> {

    @Query(value = """
            SELECT DISTINCT lr.* FROM label_rules lr
            JOIN dataset_labelrule dlr ON dlr.rule_id = lr.rule_id
            WHERE dlr.dataset_id = :datasetId
            """, nativeQuery = true)
    List<LabelRule> findLabelRulesByDatasetId(@Param("datasetId") Long datasetId);
}

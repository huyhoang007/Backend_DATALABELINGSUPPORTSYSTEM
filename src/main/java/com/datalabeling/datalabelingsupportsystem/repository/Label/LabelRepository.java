package com.datalabeling.datalabelingsupportsystem.repository.Label;

import com.datalabeling.datalabelingsupportsystem.pojo.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
        Optional<Label> findByLabelName(String labelName);

        List<Label> findByIsActiveTrue();

        @Query(value = """
                        SELECT DISTINCT l.* FROM labels l
                        JOIN labelrule_label lrl ON lrl.label_id = l.label_id
                        JOIN dataset_labelrule dlr ON dlr.rule_id = lrl.rule_id
                        WHERE dlr.dataset_id = :datasetId
                        AND l.is_active = true
                        """, nativeQuery = true)
        List<Label> findLabelsByDatasetId(@Param("datasetId") Long datasetId);

        @Query(value = """
                        SELECT lr.rule_content FROM label_rules lr
                        JOIN dataset_labelrule dlr ON dlr.rule_id = lr.rule_id
                        WHERE dlr.dataset_id = :datasetId
                        """, nativeQuery = true)
        List<String> findGuideUrlsByDatasetId(@Param("datasetId") Long datasetId);

        boolean existsByShortcutKey(String shortcutKey);
}

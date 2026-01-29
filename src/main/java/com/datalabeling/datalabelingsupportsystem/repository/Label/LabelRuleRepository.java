package com.datalabeling.datalabelingsupportsystem.repository.Label;

import com.datalabeling.datalabelingsupportsystem.pojo.LabelRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRuleRepository extends JpaRepository<LabelRule, Long> {
}

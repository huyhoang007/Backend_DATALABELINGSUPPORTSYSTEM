package com.datalabeling.datalabelingsupportsystem.repository.Label;

import com.datalabeling.datalabelingsupportsystem.pojo.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findByLabelName(String labelName);

    List<Label> findByIsActiveTrue();

    boolean existsByShortcutKey(String shortcutKey);
}

package com.datalabeling.datalabelingsupportsystem.service.Label;

import com.datalabeling.datalabelingsupportsystem.dto.request.Label.CreateLabelRuleRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Label.UpdateLabelRuleRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelRuleResponse;
import com.datalabeling.datalabelingsupportsystem.exception.ResourceNotFoundException;
import com.datalabeling.datalabelingsupportsystem.pojo.Label;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRule;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRuleLabel;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRuleLabelId;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleLabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelRuleService {

    private final LabelRuleRepository labelRuleRepository;
    private final LabelRepository labelRepository;
    private final LabelRuleLabelRepository labelRuleLabelRepository;

    @Transactional
    public LabelRuleResponse createRule(CreateLabelRuleRequest request) {
        LabelRule rule = LabelRule.builder()
                .name(request.getName())
                .ruleContent(request.getRuleContent())
                .build();

        LabelRule saved = labelRuleRepository.save(rule);

        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            attachLabels(saved.getRuleId(), request.getLabelIds());
        }

        return mapToResponse(saved);
    }

    public LabelRuleResponse getById(Long id) {
        LabelRule rule = labelRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LabelRule not found"));
        return mapToResponse(rule);
    }

    @Transactional
    public LabelRuleResponse updateRule(Long id, UpdateLabelRuleRequest request) {
        LabelRule rule = labelRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LabelRule not found"));

        rule.setName(request.getName());
        rule.setRuleContent(request.getRuleContent());

        if (request.getLabelIds() != null) {
            replaceLabels(id, request.getLabelIds());
        }

        LabelRule updated = labelRuleRepository.save(rule);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteRule(Long id) {
        LabelRule rule = labelRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LabelRule not found"));
        labelRuleRepository.delete(rule);
    }

    @Transactional
    public void attachLabels(Long ruleId, Set<Long> labelIds) {
        LabelRule rule = labelRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("LabelRule not found"));

        Set<Label> labels = labelRepository.findAllById(labelIds).stream().collect(Collectors.toSet());

        for (Label l : labels) {
            LabelRuleLabelId id = new LabelRuleLabelId(rule.getRuleId(), l.getLabelId());
            if (labelRuleLabelRepository.existsById(id)) continue;
            LabelRuleLabel link = new LabelRuleLabel(rule, l);
            labelRuleLabelRepository.save(link);
        }
    }

    @Transactional
    public void detachLabel(Long ruleId, Long labelId) {
        LabelRuleLabelId id = new LabelRuleLabelId(ruleId, labelId);
        if (labelRuleLabelRepository.existsById(id)) {
            labelRuleLabelRepository.deleteById(id);
        }
    }

    @Transactional
    public void replaceLabels(Long ruleId, Set<Long> labelIds) {
        // delete existing
        labelRuleLabelRepository.findByIdRuleId(ruleId).forEach(labelRuleLabelRepository::delete);
        // attach new
        attachLabels(ruleId, labelIds);
    }

    private LabelRuleResponse mapToResponse(LabelRule rule) {
        Set<LabelResponse> labels = rule.getLabels().stream().map(l ->
                LabelResponse.builder()
                        .labelId(l.getLabelId())
                        .labelName(l.getLabelName())
                        .colorCode(l.getColorCode())
                        .labelType(l.getLabelType())
                        .description(l.getDescription())
                        .shortcutKey(l.getShortcutKey())
                        .isActive(l.getIsActive())
                        .build()
        ).collect(Collectors.toSet());

        return LabelRuleResponse.builder()
                .ruleId(rule.getRuleId())
                .name(rule.getName())
                .ruleContent(rule.getRuleContent())
                .labels(labels)
                .build();
    }
}

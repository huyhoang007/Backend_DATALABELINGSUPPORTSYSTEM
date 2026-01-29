package com.datalabeling.datalabelingsupportsystem.service.Label;

import com.datalabeling.datalabelingsupportsystem.dto.request.Label.CreateLabelRuleRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelRuleResponse;
import com.datalabeling.datalabelingsupportsystem.exception.ResourceNotFoundException;
import com.datalabeling.datalabelingsupportsystem.pojo.Label;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRule;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRuleLabel;
import com.datalabeling.datalabelingsupportsystem.pojo.LabelRuleLabelId;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleLabelRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelRuleServiceTest {

    @Mock
    private LabelRuleRepository labelRuleRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private LabelRuleLabelRepository labelRuleLabelRepository;

    @InjectMocks
    private LabelRuleService labelRuleService;

    private LabelRule testRule;
    private Label testLabel;
    private CreateLabelRuleRequest createRequest;

    @BeforeEach
    void setUp() {
        testLabel = Label.builder()
                .labelId(1L)
                .labelName("Test Label")
                .colorCode("#FF0000")
                .labelType("BOUNDING_BOX")
                .isActive(true)
                .build();

        testRule = LabelRule.builder()
                .ruleId(1L)
                .name("Test Rule")
                .ruleContent("Test content")
                .labelLinks(new HashSet<>())
                .build();

        createRequest = new CreateLabelRuleRequest();
        createRequest.setName("Test Rule");
        createRequest.setRuleContent("Test content");
        createRequest.setLabelIds(Set.of(1L));
    }

    @Test
    void testCreateRule_Success() {
        when(labelRuleRepository.save(any(LabelRule.class))).thenReturn(testRule);
        when(labelRepository.findAllById(any())).thenReturn(new java.util.ArrayList<>(Set.of(testLabel)));
        when(labelRuleLabelRepository.existsById(any())).thenReturn(false);
        when(labelRuleLabelRepository.save(any())).thenReturn(new LabelRuleLabel(testRule, testLabel));

        LabelRuleResponse response = labelRuleService.createRule(createRequest);

        assertNotNull(response);
        assertEquals("Test Rule", response.getName());
        verify(labelRuleRepository).save(any(LabelRule.class));
    }

    @Test
    void testGetById_Success() {
        when(labelRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        LabelRuleResponse response = labelRuleService.getById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getRuleId());
    }

    @Test
    void testGetById_NotFound() {
        when(labelRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> labelRuleService.getById(1L));
    }

    @Test
    void testDeleteRule_Success() {
        when(labelRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        labelRuleService.deleteRule(1L);

        verify(labelRuleRepository).delete(testRule);
    }

    @Test
    void testDeleteRule_NotFound() {
        when(labelRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> labelRuleService.deleteRule(1L));
    }

    @Test
    void testAttachLabels_Success() {
        when(labelRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(labelRepository.findAllById(any())).thenReturn(new java.util.ArrayList<>(Set.of(testLabel)));
        when(labelRuleLabelRepository.existsById(any())).thenReturn(false);

        labelRuleService.attachLabels(1L, Set.of(1L));

        verify(labelRuleLabelRepository, times(1)).save(any(LabelRuleLabel.class));
    }

    @Test
    void testAttachLabels_RuleNotFound() {
        when(labelRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> labelRuleService.attachLabels(1L, Set.of(1L)));
    }

    @Test
    void testDetachLabel_Success() {
        LabelRuleLabelId id = new LabelRuleLabelId(1L, 1L);
        when(labelRuleLabelRepository.existsById(id)).thenReturn(true);

        labelRuleService.detachLabel(1L, 1L);

        verify(labelRuleLabelRepository).deleteById(id);
    }

    @Test
    void testReplaceLabels_Success() {
        when(labelRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(labelRuleLabelRepository.findByIdRuleId(1L)).thenReturn(new java.util.ArrayList<>());
        when(labelRepository.findAllById(any())).thenReturn(new java.util.ArrayList<>(Set.of(testLabel)));
        when(labelRuleLabelRepository.existsById(any())).thenReturn(false);

        labelRuleService.replaceLabels(1L, Set.of(1L));

        verify(labelRuleRepository).findById(1L);
        verify(labelRuleLabelRepository).findByIdRuleId(1L);
    }
}

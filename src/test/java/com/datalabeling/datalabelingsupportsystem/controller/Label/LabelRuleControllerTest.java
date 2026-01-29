package com.datalabeling.datalabelingsupportsystem.controller.Label;

import com.datalabeling.datalabelingsupportsystem.dto.request.Label.AttachLabelsRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Label.CreateLabelRuleRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelRuleResponse;
import com.datalabeling.datalabelingsupportsystem.exception.ResourceNotFoundException;
import com.datalabeling.datalabelingsupportsystem.service.Label.LabelRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class LabelRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LabelRuleService labelRuleService;

    private CreateLabelRuleRequest createRequest;
    private LabelRuleResponse ruleResponse;

    @BeforeEach
    void setUp() {
        createRequest = new CreateLabelRuleRequest();
        createRequest.setName("Test Rule");
        createRequest.setRuleContent("Test content");
        createRequest.setLabelIds(Set.of(1L));

        ruleResponse = LabelRuleResponse.builder()
                .ruleId(1L)
                .name("Test Rule")
                .ruleContent("Test content")
                .labels(new HashSet<>())
                .build();
    }

    @Test
    void testCreateRule_Success() throws Exception {
        when(labelRuleService.createRule(any())).thenReturn(ruleResponse);

        mockMvc.perform(post("/api/label-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleId").value(1))
                .andExpect(jsonPath("$.name").value("Test Rule"));

        verify(labelRuleService).createRule(any());
    }

    @Test
    void testGetRule_Success() throws Exception {
        when(labelRuleService.getById(1L)).thenReturn(ruleResponse);

        mockMvc.perform(get("/api/label-rules/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value(1))
                .andExpect(jsonPath("$.name").value("Test Rule"));

        verify(labelRuleService).getById(1L);
    }

    @Test
    void testGetRule_NotFound() throws Exception {
        when(labelRuleService.getById(1L)).thenThrow(new ResourceNotFoundException("LabelRule not found"));

        mockMvc.perform(get("/api/label-rules/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(labelRuleService).getById(1L);
    }

    @Test
    void testDeleteRule_Success() throws Exception {
        doNothing().when(labelRuleService).deleteRule(1L);

        mockMvc.perform(delete("/api/label-rules/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(labelRuleService).deleteRule(1L);
    }

    @Test
    void testAttachLabels_Success() throws Exception {
        AttachLabelsRequest request = new AttachLabelsRequest();
        request.setLabelIds(Set.of(1L));

        doNothing().when(labelRuleService).attachLabels(1L, Set.of(1L));

        mockMvc.perform(post("/api/label-rules/1/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(labelRuleService).attachLabels(1L, Set.of(1L));
    }

    @Test
    void testDetachLabel_Success() throws Exception {
        doNothing().when(labelRuleService).detachLabel(1L, 1L);

        mockMvc.perform(delete("/api/label-rules/1/labels/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(labelRuleService).detachLabel(1L, 1L);
    }

    @Test
    void testReplaceLabels_Success() throws Exception {
        AttachLabelsRequest request = new AttachLabelsRequest();
        request.setLabelIds(Set.of(1L, 2L));

        doNothing().when(labelRuleService).replaceLabels(eq(1L), any());

        mockMvc.perform(post("/api/label-rules/1/labels/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(labelRuleService).replaceLabels(eq(1L), any());
    }

    @Test
    void testCreateRule_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/label-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }
}

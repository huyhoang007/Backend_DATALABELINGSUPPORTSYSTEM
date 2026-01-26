package com.datalabeling.datalabelingsupportsystem.controller.Policy;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.UpdatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Policies.ErrorLevel;
import com.datalabeling.datalabelingsupportsystem.service.Policy.PolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyService policyService;

    private PolicyResponse policyResponse;

    @BeforeEach
    void setUp() {
        policyResponse = PolicyResponse.builder()
                .policyId(1L)
                .errorName("Missing Label")
                .description("Data point is missing a label")
                .errorLevel(ErrorLevel.HIGH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createPolicy_Success() throws Exception {
        CreatePolicyRequest request = CreatePolicyRequest.builder()
                .errorName("Missing Label")
                .description("Data point is missing a label")
                .errorLevel(ErrorLevel.HIGH)
                .build();

        when(policyService.createPolicy(any(CreatePolicyRequest.class)))
                .thenReturn(policyResponse);

        mockMvc.perform(post("/api/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1L))
                .andExpect(jsonPath("$.errorName").value("Missing Label"))
                .andExpect(jsonPath("$.errorLevel").value("HIGH"));

        verify(policyService, times(1)).createPolicy(any(CreatePolicyRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updatePolicy_Success() throws Exception {
        UpdatePolicyRequest request = UpdatePolicyRequest.builder()
                .errorName("Updated Error Name")
                .description("Updated description")
                .errorLevel(ErrorLevel.LOW)
                .build();

        PolicyResponse updatedResponse = PolicyResponse.builder()
                .policyId(1L)
                .errorName("Updated Error Name")
                .description("Updated description")
                .errorLevel(ErrorLevel.LOW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(policyService.updatePolicy(eq(1L), any(UpdatePolicyRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/policies/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1L))
                .andExpect(jsonPath("$.errorName").value("Updated Error Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.errorLevel").value("LOW"));

        verify(policyService, times(1)).updatePolicy(eq(1L), any(UpdatePolicyRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updatePolicy_PartialUpdate() throws Exception {
        // Chỉ update description
        UpdatePolicyRequest request = UpdatePolicyRequest.builder()
                .description("Updated description only")
                .build();

        PolicyResponse updatedResponse = PolicyResponse.builder()
                .policyId(1L)
                .errorName("Missing Label")
                .description("Updated description only")
                .errorLevel(ErrorLevel.HIGH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(policyService.updatePolicy(eq(1L), any(UpdatePolicyRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/policies/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1L))
                .andExpect(jsonPath("$.description").value("Updated description only"));

        verify(policyService, times(1)).updatePolicy(eq(1L), any(UpdatePolicyRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getPolicyById_Success() throws Exception {
        when(policyService.getPolicyById(1L)).thenReturn(policyResponse);

        mockMvc.perform(get("/api/policies/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1L))
                .andExpect(jsonPath("$.errorName").value("Missing Label"));

        verify(policyService, times(1)).getPolicyById(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAllPolicies_Success() throws Exception {
        List<PolicyResponse> policies = Arrays.asList(policyResponse);
        Page<PolicyResponse> page = new PageImpl<>(policies);

        when(policyService.getAllPolicies(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/policies")
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].policyId").value(1L));

        verify(policyService, times(1)).getAllPolicies(0, 20);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getPoliciesByErrorLevel_Success() throws Exception {
        List<PolicyResponse> policies = Arrays.asList(policyResponse);
        Page<PolicyResponse> page = new PageImpl<>(policies);

        when(policyService.getPoliciesByErrorLevel(ErrorLevel.HIGH, 0, 20))
                .thenReturn(page);

        mockMvc.perform(get("/api/policies/error-level/HIGH")
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].errorLevel").value("HIGH"));

        verify(policyService, times(1)).getPoliciesByErrorLevel(ErrorLevel.HIGH, 0, 20);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deletePolicy_Success() throws Exception {
        doNothing().when(policyService).deletePolicy(1L);

        mockMvc.perform(delete("/api/policies/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Policy deleted successfully"));

        verify(policyService, times(1)).deletePolicy(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void assignPolicyToProject_Success() throws Exception {
        doNothing().when(policyService).assignPolicyToProject(1L, 1L);

        mockMvc.perform(post("/api/policies/assign")
                        .with(csrf())
                        .param("projectId", "1")
                        .param("policyId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Policy assigned to project successfully"));

        verify(policyService, times(1)).assignPolicyToProject(1L, 1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void removePolicyFromProject_Success() throws Exception {
        doNothing().when(policyService).removePolicyFromProject(1L, 1L);

        mockMvc.perform(delete("/api/policies/remove")
                        .with(csrf())
                        .param("projectId", "1")
                        .param("policyId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Policy removed from project successfully"));

        verify(policyService, times(1)).removePolicyFromProject(1L, 1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getPoliciesByProject_Success() throws Exception {
        List<PolicyResponse> policies = Arrays.asList(policyResponse);

        when(policyService.getPoliciesByProject(1L)).thenReturn(policies);

        mockMvc.perform(get("/api/policies/project/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyId").value(1L));

        verify(policyService, times(1)).getPoliciesByProject(1L);
    }

    @Test
    void updatePolicy_Unauthorized() throws Exception {
        UpdatePolicyRequest request = UpdatePolicyRequest.builder()
                .errorName("Updated")
                .build();

        mockMvc.perform(put("/api/policies/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(policyService, never()).updatePolicy(anyLong(), any(UpdatePolicyRequest.class));
    }
}

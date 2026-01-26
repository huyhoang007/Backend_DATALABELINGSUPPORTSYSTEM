package com.datalabeling.datalabelingsupportsystem.controller.Policy;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.UpdatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Policies.ErrorLevel;
import com.datalabeling.datalabelingsupportsystem.service.Policy.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Management", description = "APIs for managing labeling error policies")
@SecurityRequirement(name = "BearerAuth")
public class PolicyController {
    
    private final PolicyService policyService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Create a new policy", 
        description = "Create a new labeling error policy (Admin/Manager only)"
    )
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return ResponseEntity.ok(policyService.createPolicy(request));
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Get all policies", 
        description = "Retrieve all policies with pagination (all authenticated users)"
    )
    public ResponseEntity<Page<PolicyResponse>> getAllPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(policyService.getAllPolicies(page, size));
    }
    
    @GetMapping("/{policyId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Get policy by ID", 
        description = "Retrieve a specific policy by its ID"
    )
    public ResponseEntity<PolicyResponse> getPolicyById(@PathVariable Long policyId) {
        return ResponseEntity.ok(policyService.getPolicyById(policyId));
    }
    
    @GetMapping("/error-level/{errorLevel}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Get policies by error level", 
        description = "Filter policies by error level (LOW, MEDIUM, HIGH, CRITICAL)"
    )
    public ResponseEntity<Page<PolicyResponse>> getPoliciesByErrorLevel(
            @PathVariable ErrorLevel errorLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(policyService.getPoliciesByErrorLevel(errorLevel, page, size));
    }
    
    @PutMapping("/{policyId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Update policy", 
        description = "Update an existing policy (Admin/Manager only)"
    )
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable Long policyId,
            @Valid @RequestBody UpdatePolicyRequest request) {
        return ResponseEntity.ok(policyService.updatePolicy(policyId, request));
    }
    
    @DeleteMapping("/{policyId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Delete policy", 
        description = "Delete a policy permanently (Admin only)"
    )
    public ResponseEntity<String> deletePolicy(@PathVariable Long policyId) {
        policyService.deletePolicy(policyId);
        return ResponseEntity.ok("Policy deleted successfully");
    }
    
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Assign policy to project", 
        description = "Link a policy to a specific project (Admin/Manager only)"
    )
    public ResponseEntity<String> assignPolicyToProject(
            @RequestParam Long projectId,
            @RequestParam Long policyId) {
        policyService.assignPolicyToProject(projectId, policyId);
        return ResponseEntity.ok("Policy assigned to project successfully");
    }
    
    @DeleteMapping("/remove")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Remove policy from project", 
        description = "Unlink a policy from a project (Admin/Manager only)"
    )
    public ResponseEntity<String> removePolicyFromProject(
            @RequestParam Long projectId,
            @RequestParam Long policyId) {
        policyService.removePolicyFromProject(projectId, policyId);
        return ResponseEntity.ok("Policy removed from project successfully");
    }
    
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(
        summary = "Get policies by project", 
        description = "Get all policies assigned to a specific project"
    )
    public ResponseEntity<List<PolicyResponse>> getPoliciesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(policyService.getPoliciesByProject(projectId));
    }
}

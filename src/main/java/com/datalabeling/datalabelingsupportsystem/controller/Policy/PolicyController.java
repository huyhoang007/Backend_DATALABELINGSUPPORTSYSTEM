package com.datalabeling.datalabelingsupportsystem.controller.Policy;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;
import com.datalabeling.datalabelingsupportsystem.service.Policy.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Policies", description = "APIs for managing policies")
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "Create policy")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        PolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all policies")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<PolicyResponse>> getAll() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @Operation(summary = "Get policy by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyResponse> getById(@PathVariable Long policyId) {
        return ResponseEntity.ok(policyService.getPolicyById(policyId));
    }

    @Operation(summary = "Update policy")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PatchMapping("/{policyId}")
    public ResponseEntity<PolicyResponse> update(@PathVariable Long policyId, @RequestBody CreatePolicyRequest request) {
        return ResponseEntity.ok(policyService.updatePolicy(policyId, request));
    }

    @Operation(summary = "Delete policy")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> delete(@PathVariable Long policyId) {
        policyService.deletePolicy(policyId);
        return ResponseEntity.noContent().build();
    }
}

package com.datalabeling.datalabelingsupportsystem.controller.Policy;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;
import com.datalabeling.datalabelingsupportsystem.service.Policy.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // MANAGER tạo policy
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PolicyResponse> createPolicy(
            @Valid @RequestBody CreatePolicyRequest request) {
        return ResponseEntity.ok(policyService.createPolicy(request));
    }

    // MANAGER + REVIEWER xem
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'REVIEWER', 'ADMIN')")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }
}

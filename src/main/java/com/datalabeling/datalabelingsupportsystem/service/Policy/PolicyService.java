package com.datalabeling.datalabelingsupportsystem.service.Policy;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.Policy;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        if (policyRepository.existsByName(request.getName())) {
            throw new RuntimeException("Policy name already exists");
        }

        Policy policy = Policy.builder()
                .name(request.getName())
                .description(request.getDescription())
                .content(request.getContent())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        policy = policyRepository.save(policy);

        return mapToResponse(policy);
    }

    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public PolicyResponse getPolicyById(Long policyId) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new RuntimeException("Policy not found"));
        return mapToResponse(policy);
    }

    @Transactional
    public PolicyResponse updatePolicy(Long policyId, CreatePolicyRequest request) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new RuntimeException("Policy not found"));

        if (request.getName() != null) policy.setName(request.getName());
        if (request.getDescription() != null) policy.setDescription(request.getDescription());
        if (request.getContent() != null) policy.setContent(request.getContent());
        if (request.getStatus() != null) policy.setStatus(request.getStatus());

        policy = policyRepository.save(policy);
        return mapToResponse(policy);
    }

    @Transactional
    public void deletePolicy(Long policyId) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new RuntimeException("Policy not found"));
        policyRepository.delete(policy);
    }

    private PolicyResponse mapToResponse(Policy policy) {
        return PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .name(policy.getName())
                .description(policy.getDescription())
                .content(policy.getContent())
                .status(policy.getStatus())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}

package com.datalabeling.datalabelingsupportsystem.service.Implementation;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.Policy;
import com.datalabeling.datalabelingsupportsystem.repository.Policies.PolicyRepository;
import com.datalabeling.datalabelingsupportsystem.service.Policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository repo;

    @Override
    public PolicyResponse createPolicy(CreatePolicyRequest request) {

        repo.findByErrorName(request.getErrorName())
                .ifPresent(p -> {
                    throw new RuntimeException("Policy already exists");
                });

        Policy policy = Policy.builder()
                .errorName(request.getErrorName())
                .description(request.getDescription())
                .build();

        Policy saved = this.repo.save(policy);

        return mapToResponse(saved);
    }

    @Override
    public List<PolicyResponse> getAllPolicies() {
        return repo.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PolicyResponse mapToResponse(Policy policy) {
        return PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .errorName(policy.getErrorName())
                .description(policy.getDescription())
                .build();
    }
}

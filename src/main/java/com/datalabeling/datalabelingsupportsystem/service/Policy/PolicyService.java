package com.datalabeling.datalabelingsupportsystem.service.Policy;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;

import java.util.List;

public interface PolicyService {

    PolicyResponse createPolicy(CreatePolicyRequest request);

    List<PolicyResponse> getAllPolicies();
}

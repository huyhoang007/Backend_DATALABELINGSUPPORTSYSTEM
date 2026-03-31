package com.datalabeling.datalabelingsupportsystem.service.Policy;

import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.CreatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Policy.UpdatePolicyRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Policy.PolicyResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Policies.ErrorLevel;
import com.datalabeling.datalabelingsupportsystem.pojo.Policy;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.pojo.ProjectPolicy;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.PolicyRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Policy.ProjectPolicyRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {
    
    private final PolicyRepository policyRepository;
    private final ProjectPolicyRepository projectPolicyRepository;
    private final ProjectRepository projectRepository;
    
    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        if (policyRepository.existsByErrorName(request.getErrorName())) {
            throw new RuntimeException("Chín sách với tên lỗi này đã tồn tại");
        }
        
        Policy policy = Policy.builder()
                .errorName(request.getErrorName())
                .description(request.getDescription())
                .errorLevel(request.getErrorLevel() != null ? request.getErrorLevel() : ErrorLevel.MEDIUM)
                .build();
        
        Policy savedPolicy = policyRepository.save(policy);
        return mapToResponse(savedPolicy);
    }
    
    @Transactional
    public PolicyResponse updatePolicy(Long policyId, UpdatePolicyRequest request) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Chín sách không được tìm thấy"));
        
        if (request.getErrorName() != null && !request.getErrorName().equals(policy.getErrorName())) {
            if (policyRepository.existsByErrorName(request.getErrorName())) {
                throw new RuntimeException("Chín sách với tên lỗi này đã tồn tại");
            }
            policy.setErrorName(request.getErrorName());
        }
        
        if (request.getDescription() != null) {
            policy.setDescription(request.getDescription());
        }
        
        if (request.getErrorLevel() != null) {
            policy.setErrorLevel(request.getErrorLevel());
        }
        
        Policy updatedPolicy = policyRepository.save(policy);
        return mapToResponse(updatedPolicy);
    }
    
    public Page<PolicyResponse> getAllPolicies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return policyRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }
    
    public Page<PolicyResponse> getPoliciesByErrorLevel(ErrorLevel errorLevel, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return policyRepository.findByErrorLevel(errorLevel, pageable)
                .map(this::mapToResponse);
    }
    
    public PolicyResponse getPolicyById(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Chín sách không được tìm thấy"));
        return mapToResponse(policy);
    }
    
    @Transactional
    public void deletePolicy(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Chín sách không được tìm thấy"));
        policyRepository.delete(policy);
    }
    
    // ===== Project Policy Management =====
    
    @Transactional
    public void assignPolicyToProject(Long projectId, Long policyId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không được tìm thấy"));
        
        // Kiểm tra project status: không được gán policy cho project COMPLETED
        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new RuntimeException("Dự án đã HOÀN THÀNH và bị khóa. Chỉ cho phép các hoạt động xuất.");
        }
        
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Chính sách không được tìm thấy"));
        
        if (projectPolicyRepository.existsByProjectAndPolicy(project, policy)) {
            throw new RuntimeException("Chín sách này đã được gán cho dự án rồi");
        }
        
        ProjectPolicy projectPolicy = ProjectPolicy.builder()
                .project(project)
                .policy(policy)
                .build();
        
        projectPolicyRepository.save(projectPolicy);
    }
    
    @Transactional
    public void removePolicyFromProject(Long projectId, Long policyId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không được tìm thấy"));
        
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Chính sách không được tìm thấy"));
        
        // ✅ SỬA: Tìm ProjectPolicy rồi xóa
        ProjectPolicy projectPolicy = projectPolicyRepository.findByProjectAndPolicy(project, policy)
                .orElseThrow(() -> new RuntimeException("Chín sách này chưa được gán cho dự án"));
        
        projectPolicyRepository.delete(projectPolicy);
    }
    
    public List<PolicyResponse> getPoliciesByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không được tìm thấy"));
        
        return projectPolicyRepository.findByProject(project)
                .stream()
                .map(pp -> mapToResponse(pp.getPolicy()))
                .collect(Collectors.toList());
    }
    
    private PolicyResponse mapToResponse(Policy policy) {
        return PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .errorName(policy.getErrorName())
                .description(policy.getDescription())
                .errorLevel(policy.getErrorLevel())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}

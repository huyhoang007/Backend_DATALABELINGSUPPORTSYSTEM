package com.datalabeling.datalabelingsupportsystem.service.Label;

import com.datalabeling.datalabelingsupportsystem.dto.request.Label.CreateLabelRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelResponse;
import com.datalabeling.datalabelingsupportsystem.pojo.Label;
import com.datalabeling.datalabelingsupportsystem.repository.Label.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    @Transactional
    public LabelResponse createLabel(CreateLabelRequest request) {
        // Validate unique label name
        if (labelRepository.findByLabelName(request.getLabelName()).isPresent()) {
            throw new RuntimeException("Label name already exists");
        }

        // Validate unique shortcut key
        if (request.getShortcutKey() != null &&
                labelRepository.existsByShortcutKey((request.getShortcutKey()))) {
            throw new RuntimeException("Shortcut key already in use");
        }

        Label label = Label.builder()
                .labelName(request.getLabelName())
                .colorCode(request.getColorCode())
                .labelType(request.getLabelType())
                .description(request.getDescription())
                .shortcutKey(request.getShortcutKey())
                .isActive(true)
                .build();

        Label savedLabel = labelRepository.save(label);

        return mapToResponse(savedLabel);
    }

    public List<LabelResponse> getAllLabels() {
        return labelRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LabelResponse> getActiveLabels() {
        return labelRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LabelResponse getLabelById(Long id) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Label not found"));
        return mapToResponse(label);
    }

    @Transactional
    public LabelResponse updateLabel(Long id, CreateLabelRequest request) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Label not found"));

        label.setLabelName(request.getLabelName());
        label.setColorCode(request.getColorCode());
        label.setLabelType(request.getLabelType());
        label.setDescription(request.getDescription());
        label.setShortcutKey(request.getShortcutKey());

        Label updatedLabel = labelRepository.save(label);

        return mapToResponse(updatedLabel);
    }

    @Transactional
    public void deleteLabel(Long id) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Label not found"));
        label.setIsActive(false);
        labelRepository.save(label);
    }

    private LabelResponse mapToResponse(Label label) {
        return LabelResponse.builder()
                .labelId(label.getLabelId())
                .labelName(label.getLabelName())
                .colorCode(label.getColorCode())
                .labelType(label.getLabelType())
                .description(label.getDescription())
                .shortcutKey(label.getShortcutKey())
                .isActive(label.getIsActive())
                .build();
    }
}

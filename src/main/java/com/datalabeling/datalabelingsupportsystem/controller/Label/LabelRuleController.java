package com.datalabeling.datalabelingsupportsystem.controller.Label;

import com.datalabeling.datalabelingsupportsystem.dto.request.Label.AttachLabelsRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Label.CreateLabelRuleRequest;
import com.datalabeling.datalabelingsupportsystem.dto.request.Label.UpdateLabelRuleRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelRuleResponse;
import com.datalabeling.datalabelingsupportsystem.service.Label.LabelRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/label-rules")
@RequiredArgsConstructor
@Tag(name = "LabelRule Management")
public class LabelRuleController {

    private final LabelRuleService labelRuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Create label rule")
    public ResponseEntity<LabelRuleResponse> createRule(@Valid @RequestBody CreateLabelRuleRequest request) {
        LabelRuleResponse response = labelRuleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Get label rule by id")
    public ResponseEntity<LabelRuleResponse> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(labelRuleService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Update label rule")
    public ResponseEntity<LabelRuleResponse> updateRule(@PathVariable Long id, @Valid @RequestBody UpdateLabelRuleRequest request) {
        return ResponseEntity.ok(labelRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Delete label rule")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        labelRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/labels")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Attach labels to rule")
    public ResponseEntity<Void> attachLabels(@PathVariable Long id, @Valid @RequestBody AttachLabelsRequest request) {
        labelRuleService.attachLabels(id, request.getLabelIds());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{id}/labels/{labelId}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Detach a label from rule")
    public ResponseEntity<Void> detachLabel(@PathVariable Long id, @PathVariable Long labelId) {
        labelRuleService.detachLabel(id, labelId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/labels/bulk")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Replace labels for a rule atomically")
    public ResponseEntity<Void> replaceLabels(@PathVariable Long id, @Valid @RequestBody AttachLabelsRequest request) {
        labelRuleService.replaceLabels(id, request.getLabelIds());
        return ResponseEntity.noContent().build();
    }
}

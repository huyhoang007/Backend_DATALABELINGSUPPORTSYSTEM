package com.datalabeling.datalabelingsupportsystem.controller.Label;

import com.datalabeling.datalabelingsupportsystem.dto.request.Label.CreateLabelRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.Label.LabelResponse;
import com.datalabeling.datalabelingsupportsystem.service.Label.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
@Tag(name = "Label Management")
public class LabelController {
    
    private final LabelService labelService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Create new label")
    public ResponseEntity<LabelResponse> createLabel(@Valid @RequestBody CreateLabelRequest request) {
        LabelResponse response = labelService.createLabel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Get all labels")
    public ResponseEntity<List<LabelResponse>> getAllLabels() {
        List<LabelResponse> labels = labelService.getAllLabels();
        return ResponseEntity.ok(labels);
    }
    
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Get active labels only")
    public ResponseEntity<List<LabelResponse>> getActiveLabels() {
        List<LabelResponse> labels = labelService.getActiveLabels();
        return ResponseEntity.ok(labels);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Get label by ID")
    public ResponseEntity<LabelResponse> getLabelById(@PathVariable Long id) {
        LabelResponse label = labelService.getLabelById(id);
        return ResponseEntity.ok(label);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Update label")
    public ResponseEntity<LabelResponse> updateLabel(
            @PathVariable Long id,
            @Valid @RequestBody CreateLabelRequest request) {
        LabelResponse response = labelService.updateLabel(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    @Operation(summary = "Soft delete label")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id) {
        labelService.deleteLabel(id);
        return ResponseEntity.noContent().build();
    }
}

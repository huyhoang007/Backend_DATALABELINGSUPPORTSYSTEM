package com.datalabeling.datalabelingsupportsystem.controller.DataSet;

import com.datalabeling.datalabelingsupportsystem.dto.request.DataSet.UpdateDatasetRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.DataItem.DataItemResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.DataSet.DatasetResponse;
import com.datalabeling.datalabelingsupportsystem.service.DataSet.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DatasetController {

    private final DatasetService datasetService;

    @Operation(summary = "Upload batch mới vào project")
    @PostMapping(value = "/projects/{projectId}/datasets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DatasetResponse> createDataset(
            @PathVariable Long projectId,
            @Parameter(description = "Tên batch", example = "Human_Images_v1", required = true) @RequestParam("batch_name") String batchName,
            @Parameter(description = "Danh sách file ảnh (PNG, JPG, PDF, CSV)", required = true) @RequestPart("files") List<MultipartFile> files)
            throws IOException {

        DatasetResponse response = datasetService.createDataset(projectId, batchName, files);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Lấy danh sách batch của project")
    @GetMapping("/projects/{projectId}/datasets")
    public ResponseEntity<List<DatasetResponse>> getDatasetsByProject(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(datasetService.getDatasetsByProject(projectId));
    }

    @Operation(summary = "Lấy toàn bộ ảnh active trong 1 batch")
    @GetMapping("/datasets/{datasetId}/items")
    public ResponseEntity<List<DataItemResponse>> getDatasetItems(
            @PathVariable Long datasetId) {
        return ResponseEntity.ok(datasetService.getActiveItemsByDataset(datasetId));
    }

    @Operation(summary = "Cập nhật tên batch - chỉ khi status PENDING")
    @PatchMapping("/datasets/{datasetId}")
    public ResponseEntity<DatasetResponse> updateDataset(
            @PathVariable Long datasetId,
            @Valid @RequestBody UpdateDatasetRequest request) {
        return ResponseEntity.ok(datasetService.updateDatasetName(datasetId, request));
    }

    @Operation(summary = "Thêm ảnh vào batch hiện có")
    @PostMapping(value = "/datasets/{datasetId}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DatasetResponse> addItemsToDataset(
            @PathVariable Long datasetId,
            @Parameter(description = "Danh sách file ảnh bổ sung", required = true) @RequestPart("files") List<MultipartFile> files)
            throws IOException {
        return ResponseEntity.ok(datasetService.addItemsToDataset(datasetId, files));
    }

    @Operation(summary = "Soft delete 1 ảnh")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> softDeleteItem(@PathVariable Long itemId) {
        datasetService.softDeleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}

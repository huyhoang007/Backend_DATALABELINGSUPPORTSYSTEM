package com.datalabeling.datalabelingsupportsystem.controller.DataSet;

import com.datalabeling.datalabelingsupportsystem.dto.request.DataSet.UpdateDatasetRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.DataItem.DataItemResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.DataSet.DatasetResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Export.DatasetExportResponse;
import com.datalabeling.datalabelingsupportsystem.enums.Reviewing.ReviewingStatus;
import com.datalabeling.datalabelingsupportsystem.service.DataSet.DatasetExportService;
import com.datalabeling.datalabelingsupportsystem.service.DataSet.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DatasetController {

    private final DatasetService datasetService;
    private final DatasetExportService datasetExportService;

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

    @Operation(summary = "Xóa toàn bộ dataset")
    @DeleteMapping("/datasets/{datasetId}")
    public ResponseEntity<Void> deleteDataset(@PathVariable Long datasetId) {
        datasetService.deleteDataset(datasetId);
        return ResponseEntity.noContent().build();
    }

    // ─── EXPORT ──────────────────────────────────────────────────────────────

    @Operation(summary = "Export dataset dạng JSON", description = "Trả về file JSON chứa toàn bộ ảnh và annotation của dataset. "
            +
            "Tham số status: APPROVED | PENDING | REJECTED | IMPROVED | ALL (mặc định ALL)")
    @GetMapping("/datasets/{datasetId}/export/json")
    public ResponseEntity<DatasetExportResponse> exportJson(
            @PathVariable Long datasetId,
            @Parameter(description = "Lọc annotation theo status (mặc định: ALL)") @RequestParam(value = "status", required = false) ReviewingStatus status) {

        DatasetExportResponse body = datasetExportService.buildExport(datasetId, status);
        String filename = "dataset_" + datasetId + "_export.json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @Operation(summary = "Export dataset dạng CSV", description = "Trả về file CSV, mỗi hàng là 1 annotation. " +
            "Tham số status: APPROVED | PENDING | REJECTED | IMPROVED | ALL (mặc định ALL)")
    @GetMapping("/datasets/{datasetId}/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable Long datasetId,
            @Parameter(description = "Lọc annotation theo status (mặc định: ALL)") @RequestParam(value = "status", required = false) ReviewingStatus status) {

        String csv = datasetExportService.buildCsv(datasetId, status);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        String filename = "dataset_" + datasetId + "_export.csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @Operation(summary = "Export dataset dạng COCO JSON", description = "Trả về file JSON theo chuẩn COCO (info, images, annotations, categories). "
            +
            "Tham số status: APPROVED | PENDING | REJECTED | IMPROVED | ALL (mặc định ALL)")
    @GetMapping("/datasets/{datasetId}/export/coco")
    public ResponseEntity<byte[]> exportCoco(
            @PathVariable Long datasetId,
            @Parameter(description = "Lọc annotation theo status (mặc định: ALL)") @RequestParam(value = "status", required = false) ReviewingStatus status)
            throws java.io.IOException {

        byte[] bytes = datasetExportService.buildCocoJson(datasetId, status);
        String filename = "dataset_" + datasetId + "_coco.json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    @Operation(summary = "Export dataset dạng YOLO (ZIP)", description = "Trả về ZIP chứa classes.txt, labels/*.txt (bbox normalize 0-1), images/*. "
            +
            "Tham số status: APPROVED | PENDING | REJECTED | IMPROVED | ALL (mặc định ALL)")
    @GetMapping("/datasets/{datasetId}/export/yolo")
    public ResponseEntity<byte[]> exportYolo(
            @PathVariable Long datasetId,
            @Parameter(description = "Lọc annotation theo status (mặc định: ALL)") @RequestParam(value = "status", required = false) ReviewingStatus status)
            throws java.io.IOException {

        byte[] bytes = datasetExportService.buildYoloZip(datasetId, status);
        String filename = "dataset_" + datasetId + "_yolo.zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(bytes);
    }

    @Operation(summary = "Export dataset dạng Pascal VOC (ZIP)", description = "Trả về ZIP chứa Annotations/*.xml và JPEGImages/*. "
            +
            "Tham số status: APPROVED | PENDING | REJECTED | IMPROVED | ALL (mặc định ALL)")
    @GetMapping("/datasets/{datasetId}/export/pascal-voc")
    public ResponseEntity<byte[]> exportPascalVoc(
            @PathVariable Long datasetId,
            @Parameter(description = "Lọc annotation theo status (mặc định: ALL)") @RequestParam(value = "status", required = false) ReviewingStatus status)
            throws java.io.IOException {

        byte[] bytes = datasetExportService.buildPascalVocZip(datasetId, status);
        String filename = "dataset_" + datasetId + "_pascal_voc.zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(bytes);
    }
}

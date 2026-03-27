package com.datalabeling.datalabelingsupportsystem.service.DataSet;

import com.datalabeling.datalabelingsupportsystem.dto.request.DataSet.UpdateDatasetRequest;
import com.datalabeling.datalabelingsupportsystem.dto.response.DataItem.DataItemResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.DataSet.DatasetResponse;
import com.datalabeling.datalabelingsupportsystem.enums.DataSet.BatchStatus;
import com.datalabeling.datalabelingsupportsystem.pojo.DataItem;
import com.datalabeling.datalabelingsupportsystem.pojo.Dataset;
import com.datalabeling.datalabelingsupportsystem.pojo.Project;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DataItemRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Project.ProjectRepository;
import com.datalabeling.datalabelingsupportsystem.service.Azure.AzureBlobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final DataItemRepository dataItemRepository;
    private final ProjectRepository projectRepository;
    private final AzureBlobService azureBlobService;

    // Hỗ trợ chỉ các định dạng ảnh
    private static final java.util.Set<String> ALLOWED_IMAGE_TYPES = java.util.Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp"
    );

    /**
     * POST /projects/{id}/datasets
     * Tạo Dataset mới với status PENDING và bulk insert DataItems
     */
    @Transactional
    public DatasetResponse createDataset(Long projectId, String batchName, List<MultipartFile> files)
            throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        // Tạo Dataset với status PENDING
        Dataset dataset = Dataset.builder()
                .project(project)
                .name(batchName)
                .status(BatchStatus.PENDING)
                .build();

        Dataset savedDataset = datasetRepository.save(dataset);

        // Upload files và bulk insert DataItems
        List<DataItem> dataItems = uploadAndCreateItems(files, savedDataset);
        dataItemRepository.saveAll(dataItems);

        return mapToDatasetResponse(savedDataset, dataItems.size());
    }

    /**
     * GET /projects/{id}/datasets
     * Trả về danh sách các Batch trong dự án
     */
    public List<DatasetResponse> getDatasetsByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }

        return datasetRepository.findByProject_ProjectId(projectId)
                .stream()
                .map(ds -> mapToDatasetResponse(ds,
                        dataItemRepository.countByDataset_DatasetIdAndIsActiveTrue(ds.getDatasetId())))
                .collect(Collectors.toList());
    }

    /**
     * GET /datasets/{dataset_id}/items
     * Trả về toàn bộ ảnh active trong 1 Batch
     */
    public List<DataItemResponse> getActiveItemsByDataset(Long datasetId) {
        if (!datasetRepository.existsById(datasetId)) {
            throw new RuntimeException("Dataset not found with id: " + datasetId);
        }

        return dataItemRepository.findByDataset_DatasetIdAndIsActiveTrue(datasetId)
                .stream()
                .map(this::mapToDataItemResponse)
                .collect(Collectors.toList());
    }


    /**
     * PATCH /datasets/{id}
     * Cập nhật tên batch - chỉ khi status == PENDING
     */
    @Transactional
    public DatasetResponse updateDatasetName(Long datasetId, UpdateDatasetRequest request) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new RuntimeException("Dataset not found with id: " + datasetId));

        if (dataset.getStatus() != BatchStatus.PENDING) {
            throw new RuntimeException(
                    "Cannot update dataset. Status must be PENDING, current status: " + dataset.getStatus());
        }

        dataset.setName(request.getBatchName());
        Dataset updated = datasetRepository.save(dataset);

        long count = dataItemRepository.countByDataset_DatasetIdAndIsActiveTrue(datasetId);
        return mapToDatasetResponse(updated, count);
    }

    /**
     * POST /datasets/{id}/items
     * Thêm ảnh vào Dataset hiện có
     */
    @Transactional
    public DatasetResponse addItemsToDataset(Long datasetId, List<MultipartFile> files) throws IOException {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new RuntimeException("Dataset not found with id: " + datasetId));

        List<DataItem> newItems = uploadAndCreateItems(files, dataset);
        dataItemRepository.saveAll(newItems);

        long totalCount = dataItemRepository.countByDataset_DatasetIdAndIsActiveTrue(datasetId);
        return mapToDatasetResponse(dataset, totalCount);
    }

    /**
     * DELETE /items/{item_id}
     * Soft delete: set is_active = false
     */
    @Transactional
    public void softDeleteItem(Long itemId) {
        DataItem item = dataItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("DataItem not found with id: " + itemId));

        item.setIsActive(false);
        dataItemRepository.save(item);
    }


    private List<DataItem> uploadAndCreateItems(List<MultipartFile> files, Dataset dataset) throws IOException {
        List<DataItem> items = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();

            // Xác thực: chỉ chấp nhận file ảnh
            validateImageFile(originalFilename, contentType);

            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String newFileName = UUID.randomUUID() + extension;
            String blobName = "project_" + dataset.getProject().getProjectId() + "/" + newFileName;

            byte[] fileBytes = file.getBytes();

            // Upload lên Azure Blob (hoặc local disk nếu dev)
            azureBlobService.uploadFile(blobName, fileBytes, contentType);

            // Đọc width/height
            Integer width = null, height = null;
            try {
                BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(fileBytes));
                if (img != null) { width = img.getWidth(); height = img.getHeight(); }
            } catch (IOException ignored) {}

            items.add(DataItem.builder()
                    .dataset(dataset)
                    .fileUrl("/uploads/" + blobName)
                    .fileName(originalFilename)
                    .fileType(resolveFileType(contentType))
                    .width(width)
                    .height(height)
                    .isActive(true)
                    .build());
        }
        return items;
    }

    /**
     * Xác thực file chỉ cho phép ảnh: PNG, JPG, JPEG, GIF, BMP, WEBP
     */
    private void validateImageFile(String filename, String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new RuntimeException("File không hợp lệ: không xác định được loại file");
        }

        if (!isValidImageType(contentType)) {
            throw new RuntimeException("File '" + filename + "' không được hỗ trợ. " +
                    "Chỉ hỗ trợ ảnh: PNG, JPG, JPEG, GIF, BMP, WEBP");
        }
    }

    /**
     * Kiểm tra content type có phải ảnh được hỗ trợ không
     */
    private boolean isValidImageType(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Chuyển content type sang loại file (PNG, JPG, GIF, BMP, WEBP)
     */
    private String resolveFileType(String contentType) {
        if (contentType == null)
            return "UNKNOWN";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "JPEG";
            case "image/png" -> "PNG";
            case "image/gif" -> "GIF";
            case "image/bmp" -> "BMP";
            case "image/webp" -> "WEBP";
            default -> "UNKNOWN";
        };
    }

    private DatasetResponse mapToDatasetResponse(Dataset dataset, long totalItems) {
        return DatasetResponse.builder()
                .datasetId(dataset.getDatasetId())
                .name(dataset.getName())
                .status(dataset.getStatus())
                .createdAt(dataset.getCreatedAt())
                .projectId(dataset.getProject().getProjectId())
                .totalItems(totalItems)
                .build();
    }

    private DataItemResponse mapToDataItemResponse(DataItem item) {
        return DataItemResponse.builder()
                .itemId(item.getItemId())
                .fileUrl(item.getFileUrl())
                .fileName(item.getFileName())
                .fileType(item.getFileType())
                .width(item.getWidth())
                .height(item.getHeight())
                .isActive(item.getIsActive())
                .build();
    }
}

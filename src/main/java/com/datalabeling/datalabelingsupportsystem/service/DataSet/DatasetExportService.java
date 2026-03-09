package com.datalabeling.datalabelingsupportsystem.service.DataSet;

import com.datalabeling.datalabelingsupportsystem.dto.response.Export.DatasetExportResponse;
import com.datalabeling.datalabelingsupportsystem.dto.response.Export.ExportAnnotationDto;
import com.datalabeling.datalabelingsupportsystem.dto.response.Export.ExportDataItemDto;
import com.datalabeling.datalabelingsupportsystem.enums.Reviewing.ReviewingStatus;
import com.datalabeling.datalabelingsupportsystem.exception.ResourceNotFoundException;
import com.datalabeling.datalabelingsupportsystem.pojo.DataItem;
import com.datalabeling.datalabelingsupportsystem.pojo.Dataset;
import com.datalabeling.datalabelingsupportsystem.pojo.Reviewing;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DataItemRepository;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.DatasetRepository;
import com.datalabeling.datalabelingsupportsystem.repository.Labeling.ReviewingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetExportService {

    private final DatasetRepository datasetRepository;
    private final DataItemRepository dataItemRepository;
    private final ReviewingRepository reviewingRepository;

    /**
     * Xây dựng DatasetExportResponse cho JSON export.
     *
     * @param datasetId    ID của dataset cần export
     * @param statusFilter null = tất cả status | APPROVED/PENDING/REJECTED/IMPROVED = lọc
     */
    @Transactional(readOnly = true)
    public DatasetExportResponse buildExport(Long datasetId, ReviewingStatus statusFilter) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));

        // Lấy tất cả DataItem thuộc dataset (kể cả inactive - để export đầy đủ lịch sử)
        List<DataItem> items = dataItemRepository.findByDataset_DatasetId(datasetId);

        // Lấy Reviewing (annotations) theo filter
        List<Reviewing> reviewings = statusFilter != null
                ? reviewingRepository.findByDataItem_Dataset_DatasetIdAndStatus(datasetId, statusFilter)
                : reviewingRepository.findByDataItem_Dataset_DatasetId(datasetId);

        // Group annotations by itemId để map nhanh
        Map<Long, List<Reviewing>> annotationsByItem = reviewings.stream()
                .collect(Collectors.groupingBy(r -> r.getDataItem().getItemId()));

        List<ExportDataItemDto> imageDtos = items.stream()
                .map(item -> {
                    List<ExportAnnotationDto> annots = annotationsByItem
                            .getOrDefault(item.getItemId(), List.of())
                            .stream()
                            .map(this::toAnnotationDto)
                            .collect(Collectors.toList());

                    return ExportDataItemDto.builder()
                            .itemId(item.getItemId())
                            .fileName(item.getFileName())
                            .fileUrl(item.getFileUrl())
                            .fileType(item.getFileType())
                            .width(item.getWidth())
                            .height(item.getHeight())
                            .annotations(annots)
                            .build();
                })
                .collect(Collectors.toList());

        int totalAnnotations = imageDtos.stream()
                .mapToInt(d -> d.getAnnotations().size())
                .sum();

        return DatasetExportResponse.builder()
                .datasetId(dataset.getDatasetId())
                .datasetName(dataset.getName())
                .status(dataset.getStatus().name())
                .createdAt(dataset.getCreatedAt())
                .projectId(dataset.getProject().getProjectId())
                .projectName(dataset.getProject().getName())
                .exportedStatus(statusFilter != null ? statusFilter.name() : "ALL")
                .totalImages(items.size())
                .totalAnnotations(totalAnnotations)
                .images(imageDtos)
                .build();
    }

    /**
     * Tạo nội dung CSV từ dataset export.
     * Mỗi hàng = 1 annotation. Header row đầu tiên.
     */
    @Transactional(readOnly = true)
    public String buildCsv(Long datasetId, ReviewingStatus statusFilter) {
        DatasetExportResponse export = buildExport(datasetId, statusFilter);

        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("dataset_id,dataset_name,item_id,file_name,file_url,width,height,")
          .append("reviewing_id,label_id,label_name,label_type,color_code,")
          .append("geometry,status,is_improved,annotator_name,reviewer_name\n");

        for (ExportDataItemDto item : export.getImages()) {
            if (item.getAnnotations().isEmpty()) {
                // Dòng ảnh không có annotation
                sb.append(csvEscape(export.getDatasetId()))
                  .append(",").append(csvEscape(export.getDatasetName()))
                  .append(",").append(csvEscape(item.getItemId()))
                  .append(",").append(csvEscape(item.getFileName()))
                  .append(",").append(csvEscape(item.getFileUrl()))
                  .append(",").append(csvEscape(item.getWidth()))
                  .append(",").append(csvEscape(item.getHeight()))
                  .append(",,,,,,,,,\n");
            } else {
                for (ExportAnnotationDto annot : item.getAnnotations()) {
                    sb.append(csvEscape(export.getDatasetId()))
                      .append(",").append(csvEscape(export.getDatasetName()))
                      .append(",").append(csvEscape(item.getItemId()))
                      .append(",").append(csvEscape(item.getFileName()))
                      .append(",").append(csvEscape(item.getFileUrl()))
                      .append(",").append(csvEscape(item.getWidth()))
                      .append(",").append(csvEscape(item.getHeight()))
                      .append(",").append(csvEscape(annot.getReviewingId()))
                      .append(",").append(csvEscape(annot.getLabelId()))
                      .append(",").append(csvEscape(annot.getLabelName()))
                      .append(",").append(csvEscape(annot.getLabelType()))
                      .append(",").append(csvEscape(annot.getColorCode()))
                      .append(",").append(csvEscape(annot.getGeometry()))
                      .append(",").append(csvEscape(annot.getStatus()))
                      .append(",").append(csvEscape(annot.getIsImproved()))
                      .append(",").append(csvEscape(annot.getAnnotatorName()))
                      .append(",").append(csvEscape(annot.getReviewerName()))
                      .append("\n");
                }
            }
        }
        return sb.toString();
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private ExportAnnotationDto toAnnotationDto(Reviewing r) {
        return ExportAnnotationDto.builder()
                .reviewingId(r.getReviewingId())
                .labelId(r.getLabel().getLabelId())
                .labelName(r.getLabel().getLabelName())
                .labelType(r.getLabel().getLabelType())
                .colorCode(r.getLabel().getColorCode())
                .geometry(r.getGeometry())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .isImproved(r.getIsImproved())
                .annotatorName(r.getAnnotator() != null ? r.getAnnotator().getFullName() : null)
                .reviewerName(r.getReviewer() != null ? r.getReviewer().getFullName() : null)
                .build();
    }

    /** Bao giá trị trong dấu nháy kép nếu chứa dấu phẩy hoặc xuống dòng. */
    private String csvEscape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

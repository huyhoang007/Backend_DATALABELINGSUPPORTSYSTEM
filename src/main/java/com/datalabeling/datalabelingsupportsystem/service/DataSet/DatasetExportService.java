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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class DatasetExportService {

    private final DatasetRepository datasetRepository;
    private final DataItemRepository dataItemRepository;
    private final ReviewingRepository reviewingRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

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

    // ─── ML export formats ────────────────────────────────────────────────────

    /**
     * Export dataset theo chuẩn COCO JSON.
     * Trả về JSON bytes theo schema COCO: info, images, annotations, categories.
     */
    @Transactional(readOnly = true)
    public byte[] buildCocoJson(Long datasetId, ReviewingStatus statusFilter) throws IOException {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));

        List<DataItem> items = dataItemRepository.findByDataset_DatasetId(datasetId);
        List<Reviewing> reviewings = statusFilter != null
                ? reviewingRepository.findByDataItem_Dataset_DatasetIdAndStatus(datasetId, statusFilter)
                : reviewingRepository.findByDataItem_Dataset_DatasetId(datasetId);

        Map<Long, List<Reviewing>> byItem = reviewings.stream()
                .collect(Collectors.groupingBy(r -> r.getDataItem().getItemId()));

        // Assign COCO category IDs preserving order of first occurrence
        Map<Long, Integer> labelToCategoryId = new LinkedHashMap<>();
        Map<Long, String> labelIdToName = new LinkedHashMap<>();
        int catId = 1;
        for (Reviewing r : reviewings) {
            Long lid = r.getLabel().getLabelId();
            if (!labelToCategoryId.containsKey(lid)) {
                labelToCategoryId.put(lid, catId++);
                labelIdToName.put(lid, r.getLabel().getLabelName());
            }
        }

        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode info = root.putObject("info");
        info.put("description", dataset.getName());
        info.put("version", "1.0");
        info.put("year", LocalDate.now().getYear());
        info.put("date_created", LocalDate.now().toString());

        root.putArray("licenses");

        ArrayNode imagesArr = root.putArray("images");
        Map<Long, Integer> itemToImgId = new LinkedHashMap<>();
        int imgId = 1;
        for (DataItem item : items) {
            itemToImgId.put(item.getItemId(), imgId);
            ObjectNode img = imagesArr.addObject();
            img.put("id", imgId);
            img.put("file_name", item.getFileName());
            img.put("width", item.getWidth() != null ? item.getWidth() : 0);
            img.put("height", item.getHeight() != null ? item.getHeight() : 0);
            imgId++;
        }

        ArrayNode annotArr = root.putArray("annotations");
        int annotId = 1;
        for (DataItem item : items) {
            int imageId = itemToImgId.get(item.getItemId());
            for (Reviewing r : byItem.getOrDefault(item.getItemId(), List.of())) {
                List<double[]> pts = parseGeometry(r.getGeometry());
                double[] bbox = pointsToBbox(pts);
                double area = bbox[2] * bbox[3];

                ObjectNode ann = annotArr.addObject();
                ann.put("id", annotId++);
                ann.put("image_id", imageId);
                ann.put("category_id", labelToCategoryId.getOrDefault(r.getLabel().getLabelId(), 0));

                ArrayNode bboxArr = ann.putArray("bbox");
                bboxArr.add(round2(bbox[0]));
                bboxArr.add(round2(bbox[1]));
                bboxArr.add(round2(bbox[2]));
                bboxArr.add(round2(bbox[3]));

                ArrayNode segOuter = ann.putArray("segmentation");
                String labelType = r.getLabel().getLabelType();
                if (!"BBOX".equalsIgnoreCase(labelType) && pts.size() >= 3) {
                    ArrayNode segInner = segOuter.addArray();
                    for (double[] pt : pts) {
                        segInner.add(round2(pt[0]));
                        segInner.add(round2(pt[1]));
                    }
                }

                ann.put("area", round2(area));
                ann.put("iscrowd", 0);
            }
        }

        ArrayNode catsArr = root.putArray("categories");
        labelToCategoryId.forEach((lid, cid) -> {
            ObjectNode cat = catsArr.addObject();
            cat.put("id", cid);
            cat.put("name", labelIdToName.get(lid));
            cat.put("supercategory", "");
        });

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }

    /**
     * Export dataset theo định dạng YOLO, đóng gói thành ZIP.
     * Cấu trúc ZIP:
     *   classes.txt          — tên class mỗi dòng, index = class_id
     *   labels/{name}.txt    — một annotation mỗi dòng: class_id cx cy w h (normalize 0-1)
     *   images/{name}        — ảnh gốc từ upload directory
     */
    @Transactional(readOnly = true)
    public byte[] buildYoloZip(Long datasetId, ReviewingStatus statusFilter) throws IOException {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));

        List<DataItem> items = dataItemRepository.findByDataset_DatasetId(datasetId);
        List<Reviewing> reviewings = statusFilter != null
                ? reviewingRepository.findByDataItem_Dataset_DatasetIdAndStatus(datasetId, statusFilter)
                : reviewingRepository.findByDataItem_Dataset_DatasetId(datasetId);

        Map<Long, List<Reviewing>> byItem = reviewings.stream()
                .collect(Collectors.groupingBy(r -> r.getDataItem().getItemId()));

        // Build ordered class list
        Map<Long, Integer> labelToClassId = new LinkedHashMap<>();
        List<String> classNames = new ArrayList<>();
        for (Reviewing r : reviewings) {
            Long lid = r.getLabel().getLabelId();
            if (!labelToClassId.containsKey(lid)) {
                labelToClassId.put(lid, classNames.size());
                classNames.add(r.getLabel().getLabelName());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // classes.txt
            zos.putNextEntry(new ZipEntry("classes.txt"));
            zos.write(String.join("\n", classNames).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            for (DataItem item : items) {
                double imgW = (item.getWidth() != null && item.getWidth() > 0) ? item.getWidth() : 1.0;
                double imgH = (item.getHeight() != null && item.getHeight() > 0) ? item.getHeight() : 1.0;

                StringBuilder labelContent = new StringBuilder();
                for (Reviewing r : byItem.getOrDefault(item.getItemId(), List.of())) {
                    List<double[]> pts = parseGeometry(r.getGeometry());
                    if (pts.isEmpty()) continue;
                    double[] bbox = pointsToBbox(pts);
                    int classId = labelToClassId.getOrDefault(r.getLabel().getLabelId(), 0);
                    double cx = (bbox[0] + bbox[2] / 2.0) / imgW;
                    double cy = (bbox[1] + bbox[3] / 2.0) / imgH;
                    double w  = bbox[2] / imgW;
                    double h  = bbox[3] / imgH;
                    labelContent.append(String.format(Locale.US, "%d %.6f %.6f %.6f %.6f%n", classId, cx, cy, w, h));
                }

                String baseName = fileBaseName(item.getFileName());
                zos.putNextEntry(new ZipEntry("labels/" + baseName + ".txt"));
                zos.write(labelContent.toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                byte[] imgBytes = readImageFile(item.getFileUrl());
                if (imgBytes != null) {
                    zos.putNextEntry(new ZipEntry("images/" + item.getFileName()));
                    zos.write(imgBytes);
                    zos.closeEntry();
                }
            }
        }
        return baos.toByteArray();
    }

    /**
     * Export dataset theo định dạng Pascal VOC, đóng gói thành ZIP.
     * Cấu trúc ZIP:
     *   Annotations/{name}.xml   — XML annotation theo chuẩn VOC
     *   JPEGImages/{name}        — ảnh gốc từ upload directory
     */
    @Transactional(readOnly = true)
    public byte[] buildPascalVocZip(Long datasetId, ReviewingStatus statusFilter) throws IOException {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));

        List<DataItem> items = dataItemRepository.findByDataset_DatasetId(datasetId);
        List<Reviewing> reviewings = statusFilter != null
                ? reviewingRepository.findByDataItem_Dataset_DatasetIdAndStatus(datasetId, statusFilter)
                : reviewingRepository.findByDataItem_Dataset_DatasetId(datasetId);

        Map<Long, List<Reviewing>> byItem = reviewings.stream()
                .collect(Collectors.groupingBy(r -> r.getDataItem().getItemId()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (DataItem item : items) {
                int imgW = item.getWidth() != null ? item.getWidth() : 0;
                int imgH = item.getHeight() != null ? item.getHeight() : 0;
                List<Reviewing> anns = byItem.getOrDefault(item.getItemId(), List.of());

                String xml = buildVocXml(item.getFileName(), imgW, imgH, anns);
                String baseName = fileBaseName(item.getFileName());
                zos.putNextEntry(new ZipEntry("Annotations/" + baseName + ".xml"));
                zos.write(xml.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                byte[] imgBytes = readImageFile(item.getFileUrl());
                if (imgBytes != null) {
                    zos.putNextEntry(new ZipEntry("JPEGImages/" + item.getFileName()));
                    zos.write(imgBytes);
                    zos.closeEntry();
                }
            }
        }
        return baos.toByteArray();
    }

    // ─── geometry & file helpers ─────────────────────────────────────────────

    /**
     * Parse geometry JSON string "[{"x":10,"y":20},...]" thành danh sách điểm [x, y].
     */
    private List<double[]> parseGeometry(String geometry) {
        if (geometry == null || geometry.isBlank()) return List.of();
        try {
            JsonNode arr = objectMapper.readTree(geometry);
            List<double[]> pts = new ArrayList<>();
            arr.forEach(n -> {
                double x = n.has("x") ? n.get("x").asDouble() : 0.0;
                double y = n.has("y") ? n.get("y").asDouble() : 0.0;
                pts.add(new double[]{x, y});
            });
            return pts;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Tính bounding box [x_min, y_min, width, height] từ danh sách điểm. */
    private double[] pointsToBbox(List<double[]> pts) {
        if (pts.isEmpty()) return new double[]{0, 0, 0, 0};
        double xMin = pts.stream().mapToDouble(p -> p[0]).min().getAsDouble();
        double yMin = pts.stream().mapToDouble(p -> p[1]).min().getAsDouble();
        double xMax = pts.stream().mapToDouble(p -> p[0]).max().getAsDouble();
        double yMax = pts.stream().mapToDouble(p -> p[1]).max().getAsDouble();
        return new double[]{xMin, yMin, xMax - xMin, yMax - yMin};
    }

    /** Build Pascal VOC XML string cho một ảnh. */
    private String buildVocXml(String fileName, int width, int height, List<Reviewing> anns) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<annotation>\n");
        xml.append("  <filename>").append(escapeXml(fileName)).append("</filename>\n");
        xml.append("  <size>\n");
        xml.append("    <width>").append(width).append("</width>\n");
        xml.append("    <height>").append(height).append("</height>\n");
        xml.append("    <depth>3</depth>\n");
        xml.append("  </size>\n");
        for (Reviewing r : anns) {
            List<double[]> pts = parseGeometry(r.getGeometry());
            if (pts.isEmpty()) continue;
            double[] bbox = pointsToBbox(pts);
            xml.append("  <object>\n");
            xml.append("    <name>").append(escapeXml(r.getLabel().getLabelName())).append("</name>\n");
            xml.append("    <pose>Unspecified</pose>\n");
            xml.append("    <truncated>0</truncated>\n");
            xml.append("    <difficult>0</difficult>\n");
            xml.append("    <bndbox>\n");
            xml.append("      <xmin>").append((int) bbox[0]).append("</xmin>\n");
            xml.append("      <ymin>").append((int) bbox[1]).append("</ymin>\n");
            xml.append("      <xmax>").append((int) (bbox[0] + bbox[2])).append("</xmax>\n");
            xml.append("      <ymax>").append((int) (bbox[1] + bbox[3])).append("</ymax>\n");
            xml.append("    </bndbox>\n");
            xml.append("  </object>\n");
        }
        xml.append("</annotation>\n");
        return xml.toString();
    }

    /**
     * Đọc file ảnh từ disk theo fileUrl.
     * fileUrl ví dụ: /uploads/project_15/image.jpg
     * Trả về null nếu file không tồn tại hoặc path không hợp lệ.
     */
    private byte[] readImageFile(String fileUrl) {
        if (fileUrl == null) return null;
        try {
            String relative = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            // Security: path must remain inside the upload directory after normalization
            Path resolved = Paths.get(relative).normalize();
            if (!resolved.startsWith(Paths.get(uploadPath).normalize())) {
                return null;
            }
            return Files.exists(resolved) ? Files.readAllBytes(resolved) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Tách tên file không có phần mở rộng. */
    private String fileBaseName(String fileName) {
        if (fileName == null) return "unknown";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /** Làm tròn 2 chữ số thập phân. */
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Escape XML special characters. */
    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

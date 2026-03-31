# 📦 Hướng Dẫn Chi Tiết: Lưu Ảnh Lên Azure Blob Storage

Tài liệu này giải thích chi tiết cách hệ thống Data Labeling lưu trữ ảnh khi upload dữ liệu lên Azure Blob Storage.

---

## 🎯 Sơ Đồ Tổng Quan

```
Frontend (React)
    ↓
  [Upload ảnh]
    ↓
Browser FormData
    ↓
POST /api/projects/{projectId}/datasets
    ↓
Backend (Spring Boot)
    ↓
DatasetController
    ↓
DatasetService.createDataset()
    ↓
AzureBlobService.uploadFile()
    ↓
Azure Blob Storage
    ↓
(Hoặc Local Disk nếu dev)
    ↓
FileProxyController serve file
    ↓
Browser display ảnh
```

---

## 📤 PHẦN 1: FRONTEND - CÁC BƯỚC UPLOAD

### 1.1 Người Dùng Chọn Ảnh

**File**: `Frontend_DATALABELINGSUPPORTSYSTEM/src/pages/Manager/UploadData.tsx`

```tsx
// Người dùng chọn files qua dialog hoặc drag-drop
const handleFiles = (newFiles: FileList | File[]) => {
  const fileArray = Array.from(newFiles);
  setFiles((prev) => [...prev, ...fileArray]);
};

// Hoặc drag-drop:
const handleDrop = async (e: React.DragEvent<HTMLDivElement>) => {
  e.preventDefault();
  // ...
  handleFiles(e.dataTransfer.files);
};
```

### 1.2 Gửi Ảnh Lên Backend

**File**: `src/pages/Manager/UploadData.tsx` - line 153

```tsx
const handleUpload = async () => {
  if (!selectedProjectId || !batchName.trim() || files.length === 0) return;
  setStatus("uploading");
  setError("");
  try {
    // Gọi API để upload
    await datasetApi.createDataset(
      Number(selectedProjectId),
      batchName.trim(),
      files,                    // ← Danh sách File từ input
    );
    setStatus("success");
  } catch (err: any) {
    setStatus("error");
    setError(err?.message || "Tải lên thất bại");
  }
};
```

### 1.3 API Client - Tạo FormData

**File**: `src/api/datasetApi.js` - line 31

```javascript
createDataset: async (projectId, batchName, files, onProgress) => {
  // Tạo FormData object
  const formData = new FormData();
  
  // Thêm batchName
  formData.append("batch_name", batchName);
  
  // Thêm tất cả files vào FormData
  files.forEach((file) => {
    formData.append("files", file);  // ← Mỗi file được thêm riêng
  });
  
  // Gửi POST request
  return await apiClient.post(
    `/api/projects/${projectId}/datasets`,  // ← Endpoint backend
    formData,
    {
      headers: { "Content-Type": "multipart/form-data" },
      ...(onProgress ? { onUploadProgress: onProgress } : {}),
    }
  );
};
```

**FormData Format**:
```
POST /api/projects/15/datasets HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="batch_name"

Human_Images_v1
------WebKitFormBoundary
Content-Disposition: form-data; name="files"; filename="photo1.jpg"
Content-Type: image/jpeg

[binary data của ảnh 1]
------WebKitFormBoundary
Content-Disposition: form-data; name="files"; filename="photo2.png"
Content-Type: image/png

[binary data của ảnh 2]
------WebKitFormBoundary--
```

---

## 🔧 PHẦN 2: BACKEND - CÁC BƯỚC XỬ LÝ

### 2.1 DatasetController - Nhận Request

**File**: `src/main/java/.../controller/DataSet/DatasetController.java` - line 34

```java
@Operation(summary = "Upload batch mới vào project")
@PostMapping(value = "/projects/{projectId}/datasets", 
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<DatasetResponse> createDataset(
    @PathVariable Long projectId,
    @RequestParam("batch_name") String batchName,
    @RequestPart("files") List<MultipartFile> files)  // ← Nhận multipart files
    throws IOException {
  
  // Gọi service để xử lý
  DatasetResponse response = datasetService.createDataset(projectId, batchName, files);
  return ResponseEntity.status(201).body(response);
}
```

**Request Validation**:
- `multipart/form-data` content-type
- Max file size: 10MB (cấu hình trong `application.yaml`)
- Max request size: 100MB

### 2.2 DatasetService - Tạo Dataset

**File**: `src/main/java/.../service/DataSet/DatasetService.java` - line 55

```java
@Transactional
public DatasetResponse createDataset(Long projectId, String batchName, 
                                     List<MultipartFile> files)
    throws IOException {
  
  // 1. Tìm project
  Project project = projectRepository.findById(projectId)
    .orElseThrow(() -> new RuntimeException("Project không tìm thấy"));

  // 2. Kiểm tra trạng thái project
  if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
    throw new RuntimeException("Project đã hoàn thành và bị khóa");
  }

  // 3. Tạo Dataset (Batch mới) với status = PENDING
  Dataset dataset = Dataset.builder()
    .project(project)
    .name(batchName)
    .status(BatchStatus.PENDING)
    .build();

  Dataset savedDataset = datasetRepository.save(dataset);
  
  // 4. Upload files và tạo DataItem
  List<DataItem> dataItems = uploadAndCreateItems(files, savedDataset);
  dataItemRepository.saveAll(dataItems);

  return mapToDatasetResponse(savedDataset, dataItems.size(), 
                              BatchStatus.PENDING.name());
}
```

**Database State sau bước này**:
```sql
INSERT INTO Dataset (project_id, name, status, created_at, updated_at)
VALUES (15, 'Human_Images_v1', 'PENDING', NOW(), NOW());
-- → datasetId = 123
```

### 2.3 DatasetService - Upload Files (Core Logic)

**File**: `src/main/java/.../service/DataSet/DatasetService.java` - line 195

```java
private List<DataItem> uploadAndCreateItems(List<MultipartFile> files, 
                                             Dataset dataset) 
    throws IOException {
  
  List<DataItem> items = new ArrayList<>();

  for (MultipartFile file : files) {
    if (file.isEmpty()) continue;

    String originalFilename = file.getOriginalFilename();    // "photo1.jpg"
    String contentType = file.getContentType();              // "image/jpeg"

    // ✓ Bước 1: Validation - Chỉ chấp nhận ảnh
    validateImageFile(originalFilename, contentType);
    // Kiểm tra: image/png, image/jpeg, image/gif, image/bmp, image/webp

    // ✓ Bước 2: Tạo tên file mới
    // Lý do: Tránh conflict (2 người upload "photo.jpg")
    String extension = (originalFilename != null && originalFilename.contains("."))
      ? originalFilename.substring(originalFilename.lastIndexOf("."))  // ".jpg"
      : "";
    
    String newFileName = UUID.randomUUID() + extension;
    // Ví dụ: "e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"

    // ✓ Bước 3: Tạo Blob Path (tường)
    String blobName = "project_" + dataset.getProject().getProjectId() 
                    + "/" + newFileName;
    // Ví dụ: "project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"

    // ✓ Bước 4: Đọc byte data của ảnh
    byte[] fileBytes = file.getBytes();  // Toàn bộ nội dung file

    // ✓ Bước 5: UPLOAD LÊN AZURE BLOB (hoặc local disk)
    azureBlobService.uploadFile(blobName, fileBytes, contentType);
    // → File được lưu ở Azure Blob Storage

    // ✓ Bước 6: Đọc kích thước ảnh (width/height)
    Integer width = null, height = null;
    try {
      BufferedImage img = ImageIO.read(
        new java.io.ByteArrayInputStream(fileBytes)
      );
      if (img != null) { 
        width = img.getWidth();      // 1920px
        height = img.getHeight();    // 1080px
      }
    } catch (IOException ignored) {}

    // ✓ Bước 7: Tạo DataItem object
    items.add(DataItem.builder()
      .dataset(dataset)
      .fileUrl("/uploads/" + blobName)
      // → Stored URL: "/uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"
      .fileName(originalFilename)   // "photo1.jpg"
      .fileType(resolveFileType(contentType))  // "JPEG"
      .width(width)
      .height(height)
      .isActive(true)
      .build());
  }

  return items;
}
```

**Validation Logic**:
```java
private boolean isValidImageType(String contentType) {
  Set<String> ALLOWED_IMAGE_TYPES = Set.of(
    "image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp"
  );
  return ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
}
```

---

## ☁️ PHẦN 3: AZURE BLOB STORAGE - LƯU TRỮ FILE

### 3.1 AzureBlobService - Upload File

**File**: `src/main/java/.../service/Azure/AzureBlobService.java` - line 20

```java
@Slf4j
@Service
public class AzureBlobService {

  private final BlobContainerClient containerClient;
  private final String localUploadPath;
  private final boolean azureEnabled;

  // Constructor: Khởi tạo kết nối Azure hoặc fallback to local
  public AzureBlobService(
    @Value("${azure.storage.connection-string:}") String connectionString,
    @Value("${azure.storage.container-name:uploads}") String containerName,
    @Value("${app.upload.path:uploads}") String uploadPath) {
    
    this.localUploadPath = uploadPath;

    // Nếu có connection string → Kết nối Azure Blob
    if (connectionString != null && !connectionString.isBlank()) {
      BlobServiceClient serviceClient = new BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient();
      
      containerClient = serviceClient.getBlobContainerClient(containerName);
      
      if (!containerClient.exists()) {
        containerClient.create();  // Tạo container nếu chưa có
      }
      
      azureEnabled = true;
      log.info("[Azure Blob] Đã kết nối với container: {}", containerName);
    } 
    // Nếu không → Sử dụng local disk (dev)
    else {
      containerClient = null;
      azureEnabled = false;
      log.info("[Azure Blob] Không có chuỗi kết nối — " +
               "sử dụng đĩa cứng cục bộ: {}", uploadPath);
    }
  }

  // Upload file
  public void uploadFile(String blobName, byte[] data, String contentType) 
    throws IOException {
    
    if (azureEnabled) {
      // ← TRƯỜNG HỢP 1: Azure Blob Storage
      
      // Lấy BlobClient cho file
      BlobClient blobClient = containerClient.getBlobClient(blobName);
      // blobName: "project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"
      
      // Upload data
      blobClient.upload(
        new ByteArrayInputStream(data),  // Stream của byte data
        data.length,                      // Kích thước
        true                              // overwrite nếu tồn tại
      );
      
      // Set Content-Type HTTP header
      blobClient.setHttpHeaders(
        new BlobHttpHeaders().setContentType(contentType)
        // content-type: "image/jpeg"
      );
      
      log.debug("[Azure Blob UPLOAD] {} ({} bytes)", blobName, data.length);
    } 
    else {
      // ← TRƯỜNG HỢP 2: Local Disk (Development)
      
      Path path = Paths.get(localUploadPath)
        .resolve(blobName)
        .normalize();
      // Ví dụ: "uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"
      
      // Tạo thư mục nếu chưa có
      Files.createDirectories(path.getParent());
      
      // Ghi file
      Files.write(path, data);
      
      log.debug("[Local Disk UPLOAD] {} ({} bytes)", path, data.length);
    }
  }

  // Download file (khi cần)
  public byte[] downloadFile(String blobName) throws IOException {
    if (azureEnabled) {
      BlobClient blobClient = containerClient.getBlobClient(blobName);
      return blobClient.exists() 
        ? blobClient.downloadContent().toBytes() 
        : null;
    } else {
      Path path = Paths.get(localUploadPath).resolve(blobName).normalize();
      return Files.exists(path) ? Files.readAllBytes(path) : null;
    }
  }
}
```

### 3.2 Azure Blob Storage Structure

Sau khi upload, Azure Blob Storage sẽ có cấu trúc như sau:

```
Azure Blob Container: "uploads"
│
├── project_15/
│   ├── e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg
│   ├── f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg
│   └── g7e9c1h3-4d5f-6g7h-1e0c-3d4e5f6g7h8i.png
│
├── project_16/
│   ├── h8f0d2i4-5e6g-7h8i-2f1d-4e5f6g7h8i9j.jpg
│   └── i9g1e3j5-6f7h-8i9j-3g2e-5f6g7h8i9j0k.png
│
└── project_17/
    └── j0h2f4k6-7g8i-9j0k-4h3f-6g7h8i9j0k1l.webp

```

**Azure Blob URL**:
```
https://<storage-account-name>.blob.core.windows.net/uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg
```

---

## 🔗 PHẦN 4: DATABASE - LƯU METADATA

### 4.1 DataItem Entity - Lưu Thông Tin Ảnh

Khi upload xong, hệ thống lưu thông tin metadata vào Database:

```sql
INSERT INTO DataItem (dataset_id, file_url, file_name, file_type, 
                      width, height, is_active, created_at, updated_at)
VALUES (
  123,  -- dataset_id (tham chiếu đến Dataset "Human_Images_v1")
  '/uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg',  -- fileUrl
  'photo1.jpg',  -- fileName (tên gốc do người dùng upload)
  'JPEG',  -- fileType
  1920,  -- width
  1080,  -- height
  true,  -- isActive
  NOW(),  -- createdAt
  NOW()   -- updatedAt
);

INSERT INTO DataItem (dataset_id, file_url, file_name, file_type, 
                      width, height, is_active, created_at, updated_at)
VALUES (
  123,
  '/uploads/project_15/f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg',
  'photo2.jpg',
  'JPEG',
  3840,
  2160,
  true,
  NOW(),
  NOW()
);
```

**DataItem Table Structure**:
```
┌─────────────────────────────────────────────────┐
│ DataItem (dữ liệu ảnh)                         │
├─────────────────────────────────────────────────┤
│ itemId (PK)         │ Long                      │
│ datasetId (FK)      │ Long                      │
│ file_url            │ String (path proxy)       │
│ file_name           │ String (original name)    │
│ file_type           │ String (JPEG, PNG, etc)   │
│ width               │ Integer                   │
│ height              │ Integer                   │
│ is_active           │ Boolean                   │
│ created_at          │ Timestamp                 │
│ updated_at          │ Timestamp                 │
└─────────────────────────────────────────────────┘

Entity File: src/main/java/.../pojo/DataItem.java
```

---

## 🌐 PHẦN 5: FRONTEND - LẤY VÀ HIỂN THỊ ẢNH

### 5.1 FileProxyController - Serve Ảnh

**File**: `src/main/java/.../controller/Azure/FileProxyController.java`

Khi Frontend gửi request để lấy ảnh:

```
GET /uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg
```

Backend xử lý:

```java
@GetMapping("/uploads/**")
public ResponseEntity<byte[]> serveFile(HttpServletRequest request) 
  throws IOException {
  
  // 1. Lấy path từ URL
  String fullPath = request.getRequestURI();
  // fullPath = "/uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"
  
  // 2. Validate: phải bắt đầu với "/uploads/"
  if (!fullPath.startsWith("/uploads/")) {
    return ResponseEntity.badRequest().build();
  }
  
  // 3. Lấy blobName (phần sau "/uploads/")
  String blobName = fullPath.substring("/uploads/".length());
  // blobName = "project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"
  
  // 4. Validate: không được chứa ".." (path traversal attack)
  if (blobName.isBlank() || blobName.contains("..")) {
    return ResponseEntity.badRequest().build();
  }

  // 5. Download file từ Azure Blob (hoặc local disk)
  byte[] bytes = azureBlobService.downloadFile(blobName);
  if (bytes == null) return ResponseEntity.notFound().build();

  // 6. Detect content type (MIME type)
  String contentType = detectContentType(blobName);
  // "image/jpeg", "image/png", etc.

  // 7. Trả file về browser
  return ResponseEntity.ok()
    .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
    // → Browser cache ảnh 6 giờ
    .contentType(MediaType.parseMediaType(contentType))
    .body(bytes);  // ← Binary data của ảnh
}

private String detectContentType(String name) {
  String lower = name.toLowerCase();
  if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) 
    return "image/jpeg";
  if (lower.endsWith(".png")) 
    return "image/png";
  if (lower.endsWith(".gif")) 
    return "image/gif";
  if (lower.endsWith(".webp")) 
    return "image/webp";
  return "application/octet-stream";
}
```

### 5.2 Frontend Hiển Thị Ảnh

Frontend React chỉ cần gán URL này vào `src` của `<img>` tag:

```jsx
// Component hiển thị ảnh
export default function ImageViewer({ dataItem }) {
  return (
    <img 
      src={dataItem.fileUrl}  
      // fileUrl = "/uploads/project_15/e5c7a9f1-..."
      alt={dataItem.fileName}
      style={{ maxWidth: "100%", height: "auto" }}
    />
  );
}

// Browser sẽ:
// 1. Gửi: GET /uploads/project_15/e5c7a9f1-...
// 2. Backend trả: byte data
// 3. Browser render ảnh
```

---

## 📊 PHẦN 6: TOÀN BỘ LUỒNG - MIND MAP

### Scenario: Upload 2 ảnh

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FRONTEND - User Action                                       │
├─────────────────────────────────────────────────────────────────┤
│ • User nhấn "Upload"                                            │
│ • Select 2 files: photo1.jpg (1920x1080), photo2.jpg (3840x2160)│
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. FRONTEND - Build FormData                                    │
├─────────────────────────────────────────────────────────────────┤
│ FormData {                                                      │
│   batch_name: "Human_Images_v1"                                 │
│   files: [<File1>, <File2>]                                    │
│ }                                                               │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. HTTP - Send Request                                          │
├─────────────────────────────────────────────────────────────────┤
│ POST /api/projects/15/datasets HTTP/1.1                         │
│ Content-Type: multipart/form-data                               │
│                                                                 │
│ [binary data của 2 ảnh]                                         │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. BACKEND - DatasetController                                  │
├─────────────────────────────────────────────────────────────────┤
│ • Receive: projectId=15, batchName, files                       │
│ • Validate: batch_name, files count                             │
│ • Call: DatasetService.createDataset()                          │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. BACKEND - DatasetService.createDataset()                     │
├─────────────────────────────────────────────────────────────────┤
│ • Find Project(15)                                              │
│ • Create Dataset {                                              │
│     project: Project(15)                                        │
│     name: "Human_Images_v1"                                     │
│     status: PENDING                                             │
│   }                                                             │
│ • Save to DB → datasetId=123                                    │
│ • Call: uploadAndCreateItems(files, dataset)                    │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. BACKEND - For Each File                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ FILE 1: photo1.jpg (1920x1080)                                  │
│ ┌─────────────────────────────────────────────┐                 │
│ │ • Get: filename="photo1.jpg"                │                 │
│ │         contentType="image/jpeg"            │                 │
│ │ • Validate: ✓ is image                       │                 │
│ │ • Generate: newFileName=UUID+".jpg"          │                 │
│ │            = "e5c7a9f1-2b3d-4e5f-...jpg"     │                 │
│ │ • Create: blobName="project_15/e5c7a9f1..." │                 │
│ │ • Read: bytes = 1920*1080*3 ≈ 6.2 MB        │                 │
│ │ • Upload: AzureBlobService.uploadFile()      │                 │
│ │ • Read: width=1920, height=1080              │                 │
│ │ • Create: DataItem {                         │                 │
│ │     fileUrl="/uploads/project_15/e5c7a9f1..." │                 │
│ │     fileName="photo1.jpg"                    │                 │
│ │     fileType="JPEG"                          │                 │
│ │     width=1920, height=1080                  │                 │
│ │   }                                          │                 │
│ └─────────────────────────────────────────────┘                 │
│                                                                 │
│ FILE 2: photo2.jpg (3840x2160)                                  │
│ ┌─────────────────────────────────────────────┐                 │
│ │ • Get: filename="photo2.jpg"                │                 │
│ │         contentType="image/jpeg"            │                 │
│ │ • Validate: ✓ is image                       │                 │
│ │ • Generate: newFileName="f6d8b0g2-3c4e-...jpg" │                 │
│ │ • Create: blobName="project_15/f6d8b0g2..." │                 │
│ │ • Read: bytes ≈ 24.8 MB                      │                 │
│ │ • Upload: AzureBlobService.uploadFile()      │                 │
│ │ • Read: width=3840, height=2160              │                 │
│ │ • Create: DataItem { ... }                   │                 │
│ └─────────────────────────────────────────────┘                 │
│                                                                 │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. AZURE BLOB STORAGE - Upload                                  │
├─────────────────────────────────────────────────────────────────┤
│ Container: uploads                                              │
│ ├── project_15/                                                 │
│ │   ├── e5c7a9f1-2b3d-4e5f-...jpg  [6.2 MB]                    │
│ │   └── f6d8b0g2-3c4e-5f6g-...jpg  [24.8 MB]                    │
│                                                                 │
│ Headers: Content-Type: image/jpeg                               │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8. DATABASE - Save Metadata                                     │
├─────────────────────────────────────────────────────────────────┤
│ INSERT INTO Dataset VALUES(                                     │
│   123, 15, 'Human_Images_v1', 'PENDING', ...                    │
│ )                                                               │
│                                                                 │
│ INSERT INTO DataItem VALUES(                                    │
│   1001, 123, '/uploads/project_15/e5c7a9f1-...', 'photo1.jpg', │
│   'JPEG', 1920, 1080, true, ...                                 │
│ )                                                               │
│                                                                 │
│ INSERT INTO DataItem VALUES(                                    │
│   1002, 123, '/uploads/project_15/f6d8b0g2-...', 'photo2.jpg', │
│   'JPEG', 3840, 2160, true, ...                                 │
│ )                                                               │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 9. BACKEND - Return Response                                    │
├─────────────────────────────────────────────────────────────────┤
│ DatasetResponse {                                               │
│   datasetId: 123                                                │
│   name: "Human_Images_v1"                                       │
│   status: "PENDING"                                             │
│   totalItems: 2                                                 │
│   createdAt: 2026-04-01T10:30:00Z                               │
│ }                                                               │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 10. FRONTEND - Display Success                                  │
├─────────────────────────────────────────────────────────────────┤
│ • status = "success"                                            │
│ • Reload datasets list                                          │
│ • Show new batch: "Human_Images_v1" (2 images)                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔑 PHẦN 7: KEY POINTS - HIỂU BIẾT LÕI

### Q1: Tại sao phải tạo UUID mới cho mỗi file?

**A**: Để tránh conflict resources
```
Scenario 1: Người A upload "photo.jpg" vào project 15
  → Lưu: project_15/photo.jpg

Scenario 2: Người B cũng upload "photo.jpg" vào project 15
  → Nếu không dùng UUID → Overwrite file Người A!

Solution: Dùng UUID
  → Người A: project_15/e5c7a9f1-2b3d-4e5f-...jpg
  → Người B: project_15/f6d8b0g2-3c4e-5f6g-...jpg
  ✓ Tách biệt file
  ✓ Không confict
```

### Q2: Frontend lưu URL "/uploads/..." có nghĩa gì?

**A**: Nó là **proxy URL**, không phải direct Azure URL
```
❌ KHÔNG: Lưu Azure direct URL
  https://myblob.blob.core.windows.net/uploads/project_15/...jpg
  → Hé lộ Azure credentials
  → Nếu Azure URL expired → ảnh broken

✓ YES: Lưu proxy URL
  /uploads/project_15/e5c7a9f1-2b3d-...jpg
  → FileProxyController xử lý
  → Backend kiểm tra permission
  → Backend download từ Azure
  → Return ảnh
  
Lợi ích:
  • Security: URL không hé lộ credentials
  • Control: Backend kiểm tra access
  • Flexibility: Sau này có thể thay đổi storage
```

### Q3: Tại sao Database lưu cả fileUrl và fileName?

**A**: Mục đích khác nhau
```
fileName: "photo1.jpg"
  → Hiển thị cho user (UI)
  → Download khi export

fileUrl: "/uploads/project_15/e5c7a9f1-..."
  → Serve ảnh trong annotator
  → Để browser cache, optimize performance
```

### Q4: Sự khác biệt giữa Azure Blob vs Local Disk?

**A**: Production vs Development
```
Production (Azure Blob):
  ✓ Scalable: không giới hạn dung lượng
  ✓ Redundant: backup tự động
  ✓ Shared: team có thể access từ many servers
  ✓ Security: IP restrictions, encryption

Development (Local Disk):
  ✓ Fast: không cần network
  ✓ Easy: setup đơn giản
  ✓ Free: không tốn chi phí
  ✗ Single machine: chỉ local access

AzureBlobService switch automatically:
  if (connectionString != null && !connectionString.isBlank()) {
    // → Use Azure Blob
  } else {
    // → Use Local Disk
  }
```

### Q5: Tại sao phải cache 6 giờ?

```
// FileProxyController.java
.cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())

Lợi ích:
  ✓ Reduce server load: Browser cache ảnh
  ✓ Faster loading: Lần 2 load ảnh không cần HTTP request
  ✓ Save bandwidth: Không transfer file lặp lại
  
Ví dụ:
  • Lần 1: GET /uploads/photo.jpg → Response 200, cache
  • Lần 2 (sau 10 phút): GET /uploads/photo.jpg 
    → Browser serve từ cache (304 Not Modified)
    → Không cần download lại
```

---

## 🎓 PHẦN 8: ÁP DỤNG THỰC TIỄN - VÍ DỤ CỤ THỂ

### Scenario: Trình Bày Cho Hội Đồng

**Slide 1: Quy Trình Upload**
```
Frontend                Backend           Azure Blob Storage
   ↓                       ↓                      ↓
  [Chọn 2 ảnh]
   • photo1.jpg
   • photo2.jpg
   ↓
  [FormData gửi]
   ─────────────────→
                     [Validate]
                      ↓
                    [Tạo UUID]
                      ↓
                    [Upload]
                      ─────────────────→ [Lưu trữ]
                                         • project_15/
                                           - UUID1.jpg
                                           - UUID2.jpg
                     [Lưu DB]
                      ↓
                    [Return]
                     ←────────────────
   [Show success]
```

**Slide 2: Bảng Comparison**

| Aspect | Frontend | Backend | Azure |
|--------|----------|---------|-------|
| **Nhiệm vụ** | Upload UI | Validation & Coordination | Storage |
| **Chi tiết** | Chọn files, FormData | Xác thực, UUID, Download | Lưu binary |
| **Format** | Browser File API | MultipartFile, byte[] | Blob Object |
| **Output** | FormData | DataItem + fileUrl | Binary data |

**Slide 3: An Toàn & Bảo Mật**

```
✓ File Validation
  • Chỉ ảnh (PNG, JPG, JPEG, GIF, BMP, WEBP)
  • File size limit: 10MB/file, 100MB/request

✓ UUID Naming
  • Không lộ tên gốc
  • Không conflict between users
  
✓ Proxy URLs
  • Không hé lộ Azure credentials
  • Backend kiểm tra permission trước serve
  
✓ Path Traversal Protection
  • Kiểm tra ".." trong path
  • Không cho escape khỏi upload folder
```

---

## 📝 TÓM TẮT

| Bước | Component | Hành Động | Output |
|------|-----------|----------|--------|
| 1 | Frontend | User chọn files | File[] |
| 2 | Frontend API | Tạo FormData | multipart/form-data |
| 3 | HTTP | Gửi request | POST body |
| 4 | DatasetController | Nhận request | List<MultipartFile> |
| 5 | DatasetService | Validate & coordinate | Dataset object |
| 6 | DatasetService | For each file... | UUID filename |
| 7 | AzureBlobService | Upload file | File yên trên Azure |
| 8 | DatasetService | Tạo DataItem | Metadata lưu DB |
| 9 | DatasetController | Return response | DatasetResponse JSON |
| 10 | Frontend | Render success | UI updated |

---

## 🔗 File Reference

**Backend Files**:
- `src/main/java/.../controller/DataSet/DatasetController.java` - Nhận upload request
- `src/main/java/.../service/DataSet/DatasetService.java` - Xử lý logic
- `src/main/java/.../service/Azure/AzureBlobService.java` - Upload Azure/Local
- `src/main/java/.../controller/Azure/FileProxyController.java` - Serve ảnh
- `src/main/java/.../pojo/DataItem.java` - Entity lưu metadata

**Frontend Files**:
- `src/pages/Manager/UploadData.tsx` - UI upload
- `src/api/datasetApi.js` - API client
- `src/api/apiClient.js` - HTTP client config

**Config Files**:
- `application.yaml` - Max file size, Azure connection string

---

**Tác Giả**: GitHub Copilot  
**Ngày**: 2026-04-01  
**Phiên Bản**: 1.0 - Chi tiết toàn bộ quy trình lưu ảnh

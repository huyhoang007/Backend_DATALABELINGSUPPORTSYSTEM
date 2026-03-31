# 🎯 CHEAT SHEET: Lưu Ảnh Trên Azure Blob - TÓM TẮT NHANH

> Tào liệu này dành để **nhanh tham khảo** mà không cần đọc toàn bộ chi tiết.

---

## 🚀 QUY TRÌNH - 6 BƯỚC

| Bước | Ai | Hành động | Output |
|------|-----|----------|--------|
| 1️⃣ | Frontend | User upload 2 ảnh | FormData with files |
| 2️⃣ | Backend Controller | Receive request | ValidateOK/ Error |
| 3️⃣ | Backend Service | Create UUID names | e5c7a9f1-...jpg |
| 4️⃣ | AzureBlobService | Upload to Azure/Local | ✓ File saved |
| 5️⃣ | Backend Service | Save DB metadata | ✓ Record created |
| 6️⃣ | Frontend | Get result | ✓ Show success msg |

---

## 📝 KEY POINTS

### Upload
```
Frontend                Backend              Azure Blob
   ↓                        ↓                   ↓
[Upload files]      [Validate]          [Upload/Store]
      +                 +                      +
[FormData]        [Create UUID]         [File saved]
      +                 +                      +
[POST request] → [Save DB] ← ─ ─ ─ ─ ─ ─ ─ ✓
```

### Download/Display
```
Frontend              Backend              Azure Blob
   ↓                    ↓                    ↓
[Img src=/...]  →  [Proxy Check]  →  [Get file]
                    [Security]           [Return bytes]
                    [Permission]              ↓
                         ←  ─ ─ ─ ─ ─ ─ ─ ─ ←
                    [Cache 6h]
                         ↓
                    [Display ảnh]
```

---

## 🔧 FILES TO KNOW

| File | Role | Key Methods |
|------|------|-------------|
| UploadData.tsx | Frontend UI | handleUpload(), setFiles() |
| datasetApi.js | Frontend API | createDataset() |
| DatasetController.java | Backend HTTP | POST /datasets |
| DatasetService.java | Business Logic | createDataset(), uploadAndCreateItems() |
| AzureBlobService.java | Cloud/Disk IO | uploadFile(), downloadFile() |
| FileProxyController.java | Backend HTTP | GET /uploads/... |

---

## 💾 DATABASE

### Dataset Table
```
┌─ datasetId: 123
├─ projectId: 15
├─ name: "Human_Images_v1"
├─ status: "PENDING"
└─ createdAt: 2026-04-01
```

### DataItem Table
```
┌─ itemId: 1001, 1002
├─ datasetId: 123
├─ fileUrl: "/uploads/project_15/e5c7a9f1..."
├─ fileName: "photo1.jpg", "photo2.jpg"
├─ fileType: "JPEG", "JPEG"
├─ width: 1920, 3840
├─ height: 1080, 2160
└─ isActive: true
```

---

## ☁️ AZURE BLOB STRUCTURE

```
Container: "uploads"
├── project_15/
│   ├── e5c7a9f1-2b3d-...jpg  [6.2 MB]
│   └── f6d8b0g2-3c4e-...jpg  [24.8 MB]
├── project_16/
│   ├── h8f0d2i4-5e6g-...jpg
│   └── ...
```

---

## 🔐 SECURITY CHECKLIST

- ✅ File validation (only images)
- ✅ UUID naming (no original name leak)
- ✅ Proxy URLs (no direct Azure URLs)
- ✅ Path traversal check (no "..")
- ✅ Size limits (10MB/file, 100MB/request)
- ✅ Content-Type headers
- ✅ Browser caching (6h)

---

## 🎯 AzureBlobService - QUICK REFERENCE

### Constructor
```java
If connectionString exists {
    → Initialize Azure Blob Client
    → Create container if needed
    → azureEnabled = true
} else {
    → Fallback to local disk
    → azureEnabled = false
}
```

### uploadFile(blobName, data, contentType)
```java
If azureEnabled {
    BlobClient.upload(stream, size, overwrite=true)
    BlobClient.setHttpHeaders(content-type)
} else {
    Files.createDirectories(path.parent)
    Files.write(path, data)
}
```

### downloadFile(blobName)
```java
If azureEnabled {
    return BlobClient.exists() ? downloadContent() : null
} else {
    return Files.exists(path) ? readAllBytes() : null
}
```

---

## 🚨 COMMON ISSUES & FIXES

| Issue | Cause | Fix |
|-------|-------|-----|
| File not found | Wrong blobName | Check format: `project_{ID}/{UUID}.ext` |
| 400 Bad Request | Invalid content-type | Only: image/jpeg, image/png, etc. |
| 10MB limit | File too large | Max 10MB/file, 100MB/request |
| File not upload | No containerName | Check azure.storage.container-name |
| Local upload fail | Folder not exist | createDirectories() auto-create |
| Broken image link | 6h cache expired | Can be issue if file deleted |

---

## 📊 PERFORMANCE

| Operation | Time |
|-----------|------|
| Upload 1MB | ~100ms |
| Upload 10MB | ~1s |
| Download + Browser cache | ~1ms (2nd time) |
| DB query (100 items) | ~10ms |

---

## 🔄 FLOW WITH ACTUAL DATA

```
USER ACTION: Upload 2 ảnh (photo1.jpg=6MB, photo2.jpg=25MB)

1. Frontend:
   UploadData.tsx → datasetApi.createDataset(projectId=15, batchName, files)
   
2. HTTP Request:
   POST /api/projects/15/datasets
   Content-Type: multipart/form-data
   batch_name=Human_Images_v1
   files=[file1_binary, file2_binary]
   
3. Backend:
   DatasetController.createDataset(15, "Human_Images_v1", [file1, file2])
   
4. Business Logic:
   a) Create Dataset → datasetId=123, status=PENDING
   b) For file1:
      - Validate: ✓ image/jpeg
      - UUID: e5c7a9f1-2b3d-...jpg
      - blobName: project_15/e5c7a9f1-...jpg
      - Upload: azureBlobService.uploadFile(...)
      - Read: width=1920, height=1080
      - Create DataItem {fileUrl=/uploads/project_15/e5c7..., ...}
   c) For file2:
      - (same process)
      - blobName: project_15/f6d8b0g2-3c4e-...jpg
   d) Save 2 DataItems to DB
   
5. Response:
   {
     "datasetId": 123,
     "name": "Human_Images_v1",
     "totalItems": 2,
     "status": "PENDING"
   }
   
6. Frontend:
   - setStatus("success")
   - Show "Upload thành công! Batch có 2 ảnh"
   - Reload datasets list
```

---

## 📱 API ENDPOINTS

### Upload
```
POST /api/projects/{projectId}/datasets
Content-Type: multipart/form-data

Parameters:
  batch_name (String)
  files (List<MultipartFile>)

Response:
  DatasetResponse {
    datasetId, name, status, totalItems, createdAt
  }
```

### Get Items
```
GET /api/datasets/{datasetId}/items

Response:
  DataItemResponse[] {
    itemId, fileUrl, fileName, fileType, width, height
  }
```

### Serve File
```
GET /uploads/project_{id}/uuid.ext

Response:
  Binary (byte[])
  
Headers:
  Content-Type: image/jpeg (auto-detect)
  Cache-Control: max-age=21600 (6h)
```

---

## 🧪 TESTING SCENARIOS

### Scenario 1: Valid Upload
```
Input: 2 PNG ảnh, < 10MB
Expected: ✓ Success, dataset created, 2 items
```

### Scenario 2: Invalid File Type
```
Input: .txt file
Expected: ✗ Error "Chỉ ảnh!"
```

### Scenario 3: File Too Large
```
Input: 15MB file
Expected: ✗ Error "Quá lớn"
```

### Scenario 4: Display Image
```
Action: Click image → GET /uploads/project_15/uuid.jpg
Expected: ✓ Image display, cached 6h
```

### Scenario 5: Access Without Permission
```
Action: Try direct Azure URL
Expected: ✗ Error (no credentials exposed)
```

---

## 🎓 VOCAB GLOSSARY

| Term | Meaning |
|------|---------|
| **Blob** | Binary Large Object (any file) |
| **Container** | Folder in Azure Blob Storage |
| **BlobClient** | Client to work with 1 file |
| **BlobContainerClient** | Client to work with container |
| **blobName** | File path in container |
| **Content-Type** | MIME type (image/jpeg, etc) |
| **Proxy URL** | Indirect URL via backend |
| **UUID** | Unique ID (prevents conflicts) |
| **FormData** | Browser multipart/form-data |
| **Stream** | Sequential data flow |
| **Soft Delete** | Mark inactive (not remove) |

---

## 🔗 CONFIGURATION

### application.yaml
```yaml
azure:
  storage:
    connection-string: "DefaultEndpointsProtocol=https;AccountName=...;..."
    container-name: "uploads"

app:
  upload:
    path: "uploads"

server:
  servlet:
    multipart:
      max-file-size: "10MB"
      max-request-size: "100MB"
```

### Azure Connection String Format
```
DefaultEndpointsProtocol=https
AccountName=myblob
AccountKey=base64encodedkey
EndpointSuffix=core.windows.net
```

---

## 📚 RELATED FILES

```
Frontend:
  src/pages/Manager/UploadData.tsx
  src/api/datasetApi.js
  src/api/apiClient.js

Backend:
  src/main/java/.../controller/DataSet/DatasetController.java
  src/main/java/.../service/DataSet/DatasetService.java
  src/main/java/.../service/Azure/AzureBlobService.java
  src/main/java/.../controller/Azure/FileProxyController.java
  src/main/java/.../pojo/DataItem.java
  src/main/java/.../pojo/Dataset.java

Config:
  application.yaml
  pom.xml (dependencies)
```

---

## ❓ Q&A RAPID

**Q: File được lưu ở đâu?**  
A: Azure Blob (prod) hoặc local disk (`uploads/` folder) - dev

**Q: Tại sao UUID?**  
A: Tránh conflict khi 2 người upload "photo.jpg"

**Q: fileUrl vs fileName?**  
A: fileUrl = proxy path, fileName = original name

**Q: Cache bao lâu?**  
A: 6 giờ (browser cache)

**Q: Soft delete là gì?**  
A: Set is_active = false (file vẫn ở Azure)

**Q: Max file size?**  
A: 10MB/file, 100MB/request

**Q: Firebase hoặc S3 được không?**  
A: Có thể (thay AzureBlobService)

---

**Tài Liệu**: Cheat Sheet Nhanh  
**Ngày**: 2026-04-01  
**Author**: GitHub Copilot

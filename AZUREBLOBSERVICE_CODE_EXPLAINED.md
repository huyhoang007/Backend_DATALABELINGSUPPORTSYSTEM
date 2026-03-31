# 📝 GIẢI THÍCH CHI TIẾT CODE: AzureBlobService.java

Tài liệu này giải thích **từng dòng code** của file `AzureBlobService.java` để bạn hiểu rõ cách hoạt động.

---

## 📂 File Location

```
src/main/java/com/datalabeling/datalabelingsupportsystem/service/Azure/AzureBlobService.java
```

---

## 🎯 Mục Đích File

```
AzureBlobService = "Dịch vụ quản lý file trên Azure Blob Storage"

1️⃣ Lưu ảnh: uploadFile(blobName, data, contentType)
2️⃣ Lấy ảnh: downloadFile(blobName)
3️⃣ Tự động detect: Azure Blob hay Local Disk?
```

---

## 💻 CODE - GIẢI THÍCH CHI TIẾT

### PHẦN 1: IMPORT & DEPENDENCY

```java
package com.datalabeling.datalabelingsupportsystem.service.Azure;

// Import từ Azure SDK
import com.azure.storage.blob.BlobClient;        // ← Client để làm việc với 1 file
import com.azure.storage.blob.BlobContainerClient; // ← Client để làm việc với container
import com.azure.storage.blob.BlobServiceClient;  // ← Client kết nối Azure Blob Service
import com.azure.storage.blob.BlobServiceClientBuilder; // ← Builder để tạo BlobServiceClient
import com.azure.storage.blob.models.BlobHttpHeaders; // ← Set HTTP headers (Content-Type)

// Import thư viện khác
import lombok.extern.slf4j.Slf4j;      // ← Logger (Slf4j + Lombok)
import org.springframework.beans.factory.annotation.Value; // ← Inject config từ properties
import org.springframework.stereotype.Service;   // ← Mark as Spring Service
import java.io.ByteArrayInputStream;     // ← Stream từ byte array
import java.io.IOException;
import java.nio.file.Files;              // ← Java Files API (local disk)
import java.nio.file.Path;
import java.nio.file.Paths;
```

---

### PHẦN 2: CLASS DECLARATION

```java
@Slf4j  // ← Lombok: Auto inject log field
        // Tương đương: private static final Logger log = LoggerFactory.getLogger(...);
@Service  // ← Spring: Register as component
          // ← Auto inject vào controllers/services
public class AzureBlobService {
```

**@Slf4j là gì?**
```java
// Với @Slf4j:
log.info("Message");

// Không @Slf4j thì phải:
private static final Logger log = LoggerFactory.getLogger(AzureBlobService.class);
log.info("Message");
```

---

### PHẦN 3: FIELDS (Thuộc Tính)

```java
public class AzureBlobService {
    
    // Field 1: Client để communicate với Azure Blob Container
    private final BlobContainerClient containerClient;
    
    // Field 2: Đường dẫn folder local (khi dev, không có Azure)
    private final String localUploadPath;
    
    // Field 3: Flag: Azure enabled hay không?
    private final boolean azureEnabled;
```

**Tại sao `final`?**
```
✓ Immutable: Các fields này được set 1 lần ở constructor, rồi không thay đổi
✓ Thread-safe: Không cần synchronize
✓ Best practice: Expressions như Deployment mà đặt config ở init time
```

---

### PHẦN 4: CONSTRUCTOR

```java
public AzureBlobService(
    // Parameter 1: Azure Connection String
    @Value("${azure.storage.connection-string:}") 
    String connectionString,
    
    // Parameter 2: Container name
    @Value("${azure.storage.container-name:uploads}") 
    String containerName,
    
    // Parameter 3: Local upload path (dev)
    @Value("${app.upload.path:uploads}") 
    String uploadPath) {
```

**@Value Annotation:**
```
${azure.storage.connection-string:}
 ↑                                 ↑
 |                                 └─ Default value (empty string)
 └─ Property name từ application.yaml

application.yaml:
  azure:
    storage:
      connection-string: "DefaultEndpointsProtocol=https;..."
      container-name: "uploads"
  app:
    upload:
      path: "uploads"
```

---

### PHẦN 5: CONSTRUCTOR - INIT LOCAL PATH

```java
public AzureBlobService(...) {
    
    // ✓ Bước 1: Lưu local path
    this.localUploadPath = uploadPath;
```

**Mục đích:**
```
Nếu không có Azure, sẽ lưu file ở:
  localUploadPath/project_15/uuid.jpg
  = "uploads/project_15/uuid.jpg"
```

---

### PHẦN 6: CONSTRUCTOR - AZURE INITIALIZATION

```java
    // ✓ Bước 2: Kiểm tra có connection string không?
    if (connectionString != null && !connectionString.isBlank()) {
        // A. Là String không null?
        //    ✓ null → không có Azure
        //    ✓ "" (empty) → không có Azure
        //    ✓ "Default...https://..." → có Azure
```

**Kiểm tra:**
```java
connectionString != null    // ← Array access safe check
                  && !connectionString.isBlank()
                     // ← Kiểm tra không phải empty/whitespace
```

---

### PHẦN 7: CONSTRUCTOR - CREATE BLOB SERVICE CLIENT

```java
        // B. Tạo BlobServiceClient (kết nối Azure)
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
            // ← Dùng Builder pattern để tạo client
            
            .connectionString(connectionString)
            // ← Setup connection string
            //   ví dụ: "DefaultEndpointsProtocol=https;AccountName=myblob;..."
            
            .buildClient();
            // ← Build và trả về client object
```

**Builder Pattern là gì?**
```java
// Thay vì:
BlobServiceClient client = new BlobServiceClient(
    connectionString, 
    proxy, 
    timeout, 
    retry, 
    ...
);

// Dùng Builder:
BlobServiceClient client = new BlobServiceClientBuilder()
    .connectionString(connectionString)
    .retryOptions(...)
    .buildClient();
    
// Lợi ích: Readable, flexible
```

---

### PHẦN 8: CONSTRUCTOR - GET CONTAINER CLIENT

```java
        // C. Lấy reference đến container "uploads"
        containerClient = serviceClient.getBlobContainerClient(containerName);
        // ← BlobContainerClient: client for "uploads" container
        // ← containerName = "uploads"
```

**Container là gì?**
```
Azure Blob Structure:
  
  ┌─ Storage Account ("myblob")
  │
  └─ Container ("uploads")
     ├─ project_15/
     │  ├─ uuid1.jpg
     │  └─ uuid2.jpg
     ├─ project_16/
     │  └─ uuid3.jpg
     ...

Container = nơi lưu các blobs (files)
```

---

### PHẦN 9: CONSTRUCTOR - CREATE CONTAINER IF NOT EXISTS

```java
        // D. Nếu container chưa tồn tại → Tạo
        if (!containerClient.exists()) {
            containerClient.create();
        }
        // ✓ Check exists
        // ✓ Create nếu cần
        
        // Lý do: Automation
        // Thay vì: Manual create container ở Azure Portal
        // → App tự create automatically
```

---

### PHẦN 10: CONSTRUCTOR - SET FLAG & LOG

```java
        // E. Đặt flag: Azure được enable
        azureEnabled = true;
        
        // F. Log thông báo
        log.info("[Azure Blob] Đã kết nối với container: {}", containerName);
        // Output: "[Azure Blob] Đã kết nối với container: uploads"
```

**Log level:**
```
log.debug()  → Chi tiết (verbose)
log.info()   → Thông tin quan trọng
log.warn()   → Cảnh báo (có vấn đề)
log.error()  → Lỗi (critical)
```

---

### PHẦN 11: CONSTRUCTOR - FALLBACK TO LOCAL

```java
    } else {
        // Không có Azure connection string → Dùng local disk
        
        containerClient = null;
        // ← Không dùng Azure → set null
        
        azureEnabled = false;
        // ← Set flag: Azure NOT enabled
        
        log.info("[Azure Blob] Không có chuỗi kết nối — " +
                 "sử dụng đĩa cứng cục bộ: {}", uploadPath);
        // Output: "[Azure Blob] Không có chuỗi kết nối — sử dụng đĩa cứng cục bộ: uploads"
    }
}
```

---

## 🔄 METHOD 1: uploadFile()

### Method Signature

```java
public void uploadFile(String blobName, byte[] data, String contentType) 
    throws IOException {
    
    // blobName: String
    //   Ví dụ: "project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"
    //   Format: "{folder}/{filename}"
    
    // data: byte[]
    //   Toàn bộ nội dung file ở dạng byte array
    //   Ví dụ: [255, 216, 255, 224, 0, 16, ...] (JPEG binary)
    
    // contentType: String
    //   MIME type của file
    //   Ví dụ: "image/jpeg", "image/png", "image/webp"
    
    // throws IOException
    //   Có thể throw exception nếu IO error
```

---

### METHOD BODY - CASE 1: AZURE ENABLED

```java
    if (azureEnabled) {
        // CASE 1: HAS Azure configuration
        
        // Step 1: Lấy BlobClient cho file
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        // containerClient: Reference đến "uploads" container
        // .getBlobClient(blobName): Get client cho file trong container
        // ← blobClient: Client để upload/download file này
        
        // Step 2: Upload file
        blobClient.upload(
            new ByteArrayInputStream(data),  // ← Stream input
            // Convert byte[] thành input stream
            // INPUT STREAM = dòng dữ liệu từ byte[]
            
            data.length,  // ← Kích thước (bytes)
            // Ví dụ: 6291456 (6 MB)
            
            true  // ← overwrite
            // true = Nếu file tồn tại → Overwrite
            // false = Nếu file tồn tại → Error
        );
        
        // Step 3: Set HTTP Headers
        blobClient.setHttpHeaders(
            new BlobHttpHeaders()
                .setContentType(contentType)
            // Content-Type: image/jpeg
            // Content-Type header: Browsers sẽ handle file correctly
            // Nếu không set → Browser coi như binary file (download)
        );
        
        // Step 4: Log
        log.debug("[Azure Blob UPLOAD] {} ({} bytes)", blobName, data.length);
        // Output: "[Azure Blob UPLOAD] project_15/e5c7a... (6291456 bytes)"
    }
```

**ByteArrayInputStream là gì?**
```java
byte[] bytes = {1, 2, 3, 4, 5};
InputStream stream = new ByteArrayInputStream(bytes);

// Sau đó upload API chỉ cần stream
// Không cần toàn bộ bytes trong memory cùng lúc
```

**Content-Type Header:**
```
Không set:
  GET /uploads/file.jpg
  Response headers:
    Content-Type: application/octet-stream (binary)
  → Browser: Download file

Có set:
  GET /uploads/file.jpg
  Response headers:
    Content-Type: image/jpeg
  → Browser: Display ảnh inline
```

---

### METHOD BODY - CASE 2: LOCAL DISK

```java
    } else {
        // CASE 2: NO Azure → Save to local disk
        
        // Step 1: Tạo Path object
        Path path = Paths.get(localUploadPath)
            // Paths.get() = Create Path từ string
            // localUploadPath = "uploads"
            // ← Result: "uploads" Path
            
            .resolve(blobName)
            // .resolve() = Append component
            // blobName = "project_15/e5c7a9f1-...jpg"
            // ← Result: "uploads/project_15/e5c7a9f1-...jpg" Path
            
            .normalize();
            // .normalize() = Xóa "." và ".." redundancies
            // Ví dụ: "uploads//project_15/./uuid.jpg" 
            //     → "uploads/project_15/uuid.jpg"
        
        // Step 2: Tạo parent directories
        Files.createDirectories(path.getParent());
        // path = "uploads/project_15/uuid.jpg"
        // path.getParent() = "uploads/project_15"
        // createDirectories() = mkdir -p
        //   ✓ Create if not exists
        //   ✓ Create parents if needed
        //   ✓ Idempotent (safe to call multiple times)
        
        // Step 3: Ghi file
        Files.write(path, data);
        // java.nio.file.Files.write()
        // ← Write byte array thành file
        // ← Tự động close stream
        
        // Step 4: Log
        log.debug("[Local Disk UPLOAD] {} ({} bytes)", path, data.length);
        // Output: "[Local Disk UPLOAD] uploads/project_15/e5c7a... (6291456 bytes)"
    }
}
```

**Path example:**
```java
Path path1 = Paths.get("uploads").resolve("project_15/file.jpg");
// Result: Path("uploads/project_15/file.jpg")
// On Windows: "uploads\project_15\file.jpg"
// On Linux:   "uploads/project_15/file.jpg"
// ← Path object handles OS differences

path1.normalize();
// "uploads/./project_15/../project_15/file.jpg"
// → "uploads/project_15/file.jpg"
```

---

## 🔄 METHOD 2: downloadFile()

### Method Signature

```java
public byte[] downloadFile(String blobName) throws IOException {
    // blobName: "project_15/uuid.jpg"
    // Return: byte[] (file content)
    //   hoặc null (nếu file không tồn tại)
```

---

### METHOD BODY - CASE 1: AZURE ENABLED

```java
    if (azureEnabled) {
        // CASE 1: Download từ Azure Blob
        
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        // ← Get client cho file
        
        return blobClient.exists()
            // Check: File tồn tại?
            
            ? blobClient.downloadContent().toBytes()
            // ✓ YES: Download content
            //   .downloadContent() = Download file
            //   .toBytes() = Convert InputStream to byte[]
            
            : null;
            // ✗ NO: Return null
    }
```

**Ternary operator:**
```java
condition ? valueIfTrue : valueIfFalse

file.exists() ? download() : null
// Nếu tồn tại: download
// Nếu không: null
```

---

### METHOD BODY - CASE 2: LOCAL DISK

```java
    else {
        // CASE 2: Download từ Local Disk
        
        Path path = Paths.get(localUploadPath).resolve(blobName).normalize();
        // ← Tạo Path (giống uploadFile)
        
        return Files.exists(path)
            // Check: File tồn tại?
            
            ? Files.readAllBytes(path)
            // ✓ YES: Read file
            //   Files.readAllBytes() = Read toàn bộ file to byte[]
            
            : null;
            // ✗ NO: Return null
    }
}
```

**Files.readAllBytes() cảnh báo:**
```
⚠️ Không nên dùng với file lớn (> 1GB)
   → Load toàn bộ vào memory

✓ Tốt cho: File nhỏ (ảnh JPG ~5MB OK)

Để tốt hơn với file lớn:
  InputStream stream = Files.newInputStream(path);
  byte[] buffer = new byte[8192];
  while (stream.read(buffer) != -1) {
    // Process chunk by chunk
  }
```

---

## 📊 COMPARISON: Methods

| Aspect | uploadFile() | downloadFile() |
|--------|--------------|----------------|
| **Input** | blobName, data, contentType | blobName |
| **Output** | void | byte[] \| null |
| **Purpose** | Save file | Read file |
| **Azure** | upload() | downloadContent() |
| **Local** | Files.write() | Files.readAllBytes() |
| **Error** | throws IOException | returns null |

---

## 🔑 KEY CONCEPTS

### 1. Stream vs Bytes

```java
// Bytes: Toàn bộ data cùng lúc
byte[] bytes = {1, 2, 3, 4, 5};
// Memory: 5 bytes

// Stream: Dòng dữ liệu (phần từng phần)
InputStream stream = new InputStream() { ... };
// Memory: Buffer ~8KB
// Có thể xử lý file 1GB không cần 1GB RAM

// Conversion:
byte[] → new ByteArrayInputStream(bytes) → InputStream
```

### 2. Configuration Injection

```java
@Value("${azure.storage.connection-string:}")
// 
// Nếu property không tồn tại: use default ("")
// 
// application.yaml:
//   azure:
//     storage:
//       connection-string: "DefaultEndpointsProtocol=https;..."
```

### 3. Conditional Logic

```java
if (azureEnabled) {
    // Azure Blob logic
} else {
    // Local Disk logic
}

// Lợi ích:
// ✓ Chỉ 1 file để manage 2 modes
// ✓ Easily switch between prod/dev
// ✓ Automated fallback
```

### 4. Exception Handling

```java
public void uploadFile(...) throws IOException {
    // throws IOException
    // Caller phải handle:
    
    try {
        azureBlobService.uploadFile(...);
    } catch (IOException e) {
        // Handle error
    }
}

// OR

public byte[] downloadFile(...) throws IOException {
    // Nếu error → throws
    // Nếu file not found → return null
}
```

---

## 📊 USAGE EXAMPLE

### Example 1: Upload Photo

```java
// DatasetService.uploadAndCreateItems()

String blobName = "project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg";
byte[] fileBytes = multipartFile.getBytes();  // Read from upload
String contentType = "image/jpeg";

azureBlobService.uploadFile(blobName, fileBytes, contentType);
// ✓ File lưu ở Azure (prod) hoặc local (dev)
```

### Example 2: Download Photo

```java
// FileProxyController.serveFile()

String blobName = "project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg";
byte[] bytes = azureBlobService.downloadFile(blobName);

if (bytes != null) {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .body(bytes);
} else {
    return ResponseEntity.notFound().build();
}
```

---

## ✅ BEST PRACTICES

### 1. Error Handling

```java
✓ uploadFile():
  try {
    azureBlobService.uploadFile(...);
  } catch (IOException e) {
    log.error("Upload failed", e);
    throw new RuntimeException("Lỗi upload");
  }

✓ downloadFile():
  byte[] bytes = azureBlobService.downloadFile(...);
  if (bytes == null) {
    return ResponseEntity.notFound().build();
  }
```

### 2. Logging

```java
// Good:
log.debug("[Azure Blob UPLOAD] {} ({} bytes)", blobName, data.length);

// Better:
log.info("[Azure Blob UPLOAD] Successfully uploaded {} ({} bytes) to container", 
         blobName, data.length);
```

### 3. Resource Management

```java
// InputStream auto-closed:
Files.write(path, data);      // ← Auto-closed
Files.readAllBytes(path);     // ← Auto-closed

// Manual if needed:
try (InputStream is = ...) {
  // Resource auto-closed after block
}
```

---

## 📝 NAMING CONVENTIONS

| Item | Format | Example |
|------|--------|---------|
| **blobName** | `project_{ID}/{UUID}.{ext}` | `project_15/e5c7a9f1-...jpg` |
| **containerName** | lowercase, no spaces | `uploads` |
| **Method names** | camelCase, verb | `uploadFile`, `downloadFile` |
| **Field names** | camelCase, noun | `containerClient`, `azureEnabled` |
| **Constants** | UPPER_CASE | `ALLOWED_IMAGE_TYPES` |

---

## 🎓 SUMMARY

| Component | Purpose |
|-----------|---------|
| **Constructor** | Initialize Azure or fallback to local |
| **uploadFile()** | Save file to Azure/Local |
| **downloadFile()** | Read file from Azure/Local |
| **@Slf4j** | Auto logging |
| **azureEnabled flag** | Switch between modes |

---

**Tài Liệu**: Chi Tiết Code AzureBlobService  
**Ngày**: 2026-04-01  
**Phiên Bản**: 1.0

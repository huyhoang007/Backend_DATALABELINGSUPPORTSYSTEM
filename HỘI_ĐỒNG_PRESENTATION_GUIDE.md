# 🎤 CÁCH TRÌNH BÀY CHO HỘI ĐỒNG: AZURE BLOB STORAGE

> Hướng dẫn cách nói chuyện với hội đồng về cách hệ thống lưu ảnh.  
> Dành cho sinh viên/kỹ sư bảo vệ/trình bày project.

---

## 📌 PHẦN MỞ ĐẦU - 30 GIÂY

**Nói đơn giản:**

> "Thưa hội đồng, hôm nay em xin phép trình bày về cách hệ thống Data Labeling **lưu trữ ảnh** khi người dùng upload dữ liệu lên nền tảng.
>
> Bản chất nó là: Người dùng upload ảnh từ máy tính → Hệ thống validate ảnh → Lưu vào **cloud storage** (Azure Blob) hoặc **local disk** (khi dev) → Lưu thông tin metadata vào database → Frontend có thể hiển thị ảnh bất kỳ lúc nào. 
>
> Điểm quan trọng là hệ thống sử dụng **UUID** (tên ngẫu nhiên duy nhất) để tránh conflict khi 2 người dùng upload cùng tên file, và sử dụng **proxy URL** để bảo mật - không hé lộ Azure credentials."

---

## 📊 PHẦN 1: KIẾN TRÚC TỔNG QUÁT - 5 PHÚT

### Slide 1: Sơ Đồ Luồng (HIGH LEVEL)

**Nói:**
> "Cấu trúc hệ thống bao gồm 4 phần chính:"

```
1️⃣ FRONTEND (React)
   ↓
2️⃣ BACKEND (Spring Boot)
   ↓
3️⃣ CLOUD STORAGE (Azure Blob)
   │
   ├─ FILE BINARY (ảnh thực)
   │
4️⃣ DATABASE (SQL)
   └─ METADATA (thông tin về ảnh)
```

**Giải thích:**
- **Frontend**: Giao diện người dùng (React)
- **Backend**: Tổng phối viên việc (Spring Boot)
- **Cloud Storage**: Lưu file thực (Azure)
- **Database**: Lưu thông tin (tên gốc, kích thước, đường dẫn)

### Slide 2: Khác Biệt Production vs Development

| Aspect | Production | Development |
|--------|-----------|-----------|
| **Storage** | ☁️ Azure Blob Cloud | 💻 Local Disk |
| **Độ Tin Cậy** | 99.9% SLA | Single machine |
| **Kích Thước** | Unlimited | Máy tính có bao nhiêu |
| **Cost** | $ mỗi GB | Free |
| **Sharing** | ✓ Team có thể access | ✗ Chỉ local |

**Nói:**
> "Trong production, chúng tôi sử dụng Azure Blob - a service của Microsoft với tính sẵn sàng 99.9%. Nhưng khi develop, chúng tôi lưu file vào local disk để không phải chi phí Azure. Hệ thống tự động detect cấu hình và chọn mode phù hợp."

---

## 📤 PHẦN 2: QUY TRÌNH UPLOAD - 8 PHÚT

### Slide 3: 6 Bước Upload

```
BƯỚC 1: User Upload
   ↓
BƯỚC 2: Frontend Tạo FormData
   ↓
BƯỚC 3: Backend Nhận & Validate
   ↓
BƯỚC 4: Tạo UUID Names
   ↓
BƯỚC 5: Upload Lên Azure/Local
   ↓
BƯỚC 6: Lưu Database & Return Success
```

### Slide 4: Bước 1 - User Upload

**Code Demo (Console):**
```javascript
// Frontend: UploadData.tsx
<input 
  type="file" 
  multiple 
  onChange={(e) => setFiles(e.target.files)}
/>
```

**Nói:**
> "Người dùng mở ứng dụng, chọn tab 'Upload dữ liệu', sau đó chọn ảnh. Hệ thống cho phép chọn nhiều file cùng lúc hoặc kéo thả."

### Slide 5: Bước 2 - FormData

**Code Demo:**
```javascript
// datasetApi.js - line 31
const formData = new FormData();
formData.append("batch_name", "Human_Images_v1");
files.forEach((file) => {
    formData.append("files", file);
});

// POST request
POST /api/projects/15/datasets
Content-Type: multipart/form-data
```

**Nói:**
> "Frontend tạo object gọi là FormData - đó là định dạng tiêu chuẩn của web để upload file. Nó tương tự như khi bạn upload ảnh lên Facebook hoặc Gmail - browser sẽ ghi tên batch và danh sách files vào FormData, rồi gửi POST request đến backend."

**Visual:**
```
Browser FormData:
┌─────────────────────────────────┐
│ batch_name: "Human_Images_v1"   │
├─────────────────────────────────┤
│ files[0]: [binary JPG 1]        │
│           photo1.jpg             │
│           size: 6.2 MB           │
├─────────────────────────────────┤
│ files[1]: [binary JPG 2]        │
│           photo2.jpg             │
│           size: 24.8 MB          │
└─────────────────────────────────┘
```

### Slide 6: Bước 3 - Backend Validate

**Code Demo:**
```java
// DatasetController.java - line 34
@PostMapping("/projects/{projectId}/datasets")
public ResponseEntity<DatasetResponse> createDataset(
    @PathVariable Long projectId,
    @RequestParam("batch_name") String batchName,
    @RequestPart("files") List<MultipartFile> files)
```

**Nói:**
> "Backend nhận FormData, kiểm tra:
>
> ✓ Batch name có được nhập không?  
> ✓ Ảnh là ảnh hợp lệ không? (PNG, JPG, GIF, ...)  
> ✓ File size không vượt quá 10MB mỗi file?  
> ✓ Tổng request size không vượt 100MB?  
>
> Nếu hợp lệ → tiếp tục bước 4. Nếu không → trả error 400."

**Validation Code:**
```java
// DatasetService.java - line 228
private boolean isValidImageType(String contentType) {
    return ALLOWED_IMAGE_TYPES.contains(
        contentType.toLowerCase()
    );
    // Kiểm tra: image/jpeg, image/png, image/gif, ...
}
```

### Slide 7: Bước 4 - Tạo UUID

**Vấn Đề:**
```
❌ SKHÔNG LÀM:
  Photo1.jpg → Lưu: photo1.jpg
  Photo1.jpg → Lưu: photo1.jpg (OVERWRITE! ❌)

✅ LÀM ĐÚNG:
  Photo1.jpg → Tạo UUID → e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg
  Photo1.jpg → Tạo UUID → f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg
  ✓ Không conflict!
```

**Code Demo:**
```java
// DatasetService.java - line 205
String newFileName = UUID.randomUUID() + extension;
// Kết quả: "e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"

String blobName = "project_" + dataset.getProject().getProjectId() 
                + "/" + newFileName;
// Kết quả: "project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg"
```

**Nói:**
> "Hệ thống tạo tên file mới bằng UUID - một ID ngẫu nhiên duy nhất. Điều này tránh trường hợp 2 người upload cùng tên 'photo.jpg' - nếu không, file của người thứ nhất sẽ bị ghi đè. Ngoài ra, UUID cũng giúp bảo vệ - người dùng không biết tên gốc của file."

**UUID Concept:**
```
UUID = Universally Unique Identifier
Xác suất 2 UUID giống nhau = 1 / 5.3×10^36
≈ gần như không bao giờ xảy ra

Ví dụ UUID:
  e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h
  f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h
  g7e9c1h3-4d5f-6g7h-1e0c-3d4e5f6g7h8i
```

### Slide 8: Bước 5 - Upload Lên Azure

**Code Demo:**
```java
// AzureBlobService.java - line 57
public void uploadFile(String blobName, byte[] data, String contentType) 
    throws IOException {
    
    if (azureEnabled) {
        // AZURE BLOB UPLOAD
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(
            new ByteArrayInputStream(data), 
            data.length, 
            true  // overwrite
        );
        blobClient.setHttpHeaders(
            new BlobHttpHeaders().setContentType(contentType)
        );
    } else {
        // LOCAL DISK UPLOAD (dev)
        Path path = Paths.get(localUploadPath)
            .resolve(blobName)
            .normalize();
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }
}
```

**Nói:**
> "Hệ thống gọi AzureBlobService - một service được thiết kế để upload file.
>
> Nếu **production** (có Azure connection string) → upload lên Azure Blob Storage  
> Nếu **development** (không có connection string) → lưu vào local disk
>
> Cả hai cách đều lưu file theo path format:
> `project_{ID}/{UUID}.{extension}`
>
> Ví dụ: `project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg`"

**Azure Blob Structure Visual:**
```
Azure Storage Account: myblob
└── Container: uploads
    ├── project_15/
    │   ├── e5c7a9f1-2b3d-...jpg  ← ảnh 1
    │   └── f6d8b0g2-3c4e-...jpg  ← ảnh 2
    ├── project_16/
    │   ├── ...
    └── project_17/
        ├── ...
```

### Slide 9: Bước 6 - Lưu Database

**Code Demo:**
```java
// DatasetService.java - line 95
items.add(DataItem.builder()
    .dataset(dataset)
    .fileUrl("/uploads/" + blobName)  // /uploads/project_15/e5c7a...
    .fileName(originalFilename)        // photo1.jpg
    .fileType(resolveFileType(contentType))  // JPEG
    .width(width)                      // 1920
    .height(height)                    // 1080
    .isActive(true)
    .build());

dataItemRepository.saveAll(dataItems);  // INSERT to DB
```

**SQL Insert:**
```sql
INSERT INTO DataItem (dataset_id, file_url, file_name, 
                      file_type, width, height, is_active)
VALUES (
    123,
    '/uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg',
    'photo1.jpg',
    'JPEG',
    1920,
    1080,
    true
);
```

**Nói:**
> "Sau khi upload file lên Azure/Local xong, hệ thống lưu **metadata** (thông tin về ảnh) vào database:
>
> - **fileUrl**: Đường dẫn proxy để frontend lấy ảnh  
> - **fileName**: Tên gốc mà người dùng upload  
> - **fileType**: Loại ảnh (JPEG, PNG, ...)  
> - **width, height**: Kích thước ảnh (pixel)  
> - **isActive**: Ảnh đang hoạt động hay đã xóa?
>
> Lưu metadata giúp frontend có thể hiển thị danh sách ảnh mà không cần download từ Azure mỗi lần."

---

## 📥 PHẦN 3: QUY TRÌNH HIỂN THỊ - 5 PHÚT

### Slide 10: Luồng Hiển Thị Ảnh

```
Frontend Load Batch
   ↓ GET /api/datasets/123/items
Database (DataItem table)
   ↓ SELECT * WHERE dataset_id = 123
Frontend Nhận List
   ↓ Render <img src="/uploads/project_15/e5c7...">
Browser Request
   ↓ GET /uploads/project_15/e5c7a9f1-...jpg
FileProxyController
   ↓ Validate path
AzureBlobService.downloadFile()
   ↓ Get file from Azure/Local
Return to Browser
   ↓ Display ảnh + Cache 6h
```

**Nói:**
> "Khi người dùng mở batch để gán nhãn:
>
> 1. Frontend gọi API GET để lấy danh sách ảnh  
> 2. Backend truy vấn Database, trả danh sách  
> 3. Frontend render HTML <img> với src = fileUrl  
> 4. Browser gửi HTTP request GET /uploads/...  
> 5. FileProxyController nhận, validate path (kiểm tra security)  
> 6. Download file từ Azure/Local  
> 7. Trả binary data + Content-Type header  
> 8. Browser hiển thị ảnh
>
> Lần lần lần lần lần đến, browser sẽ cache ảnh 6 giờ - không cần download lại."

### Slide 11: Security - Proxy URL

**Vấn Đề:**
```
❌ KHÔNG NÊN:
  Lưu direct Azure URL:
  https://myblob.blob.core.windows.net/uploads/project_15/...

Rủi ro:
  - URL chứa authentication token → Hé lộ credentials
  - Ai có URL → Có thể access file
  - URL hết hạn → Ảnh broken
  - Không kiểm tra permission

✅ NÊN:
  Lưu proxy URL:
  /uploads/project_15/e5c7a9f1-2b3d-...jpg
  
Lợi ích:
  - Không expose credentials
  - Backend kiểm tra permission từng request
  - Flexible: thay đổi storage sau
  - Có thể implement CDN
```

**Nói:**
> "Một điểm quan trọng về bảo mật: hệ thống **không lưu trực tiếp Azure URL** vào database.
>
> Thay vì lưu:
> `https://myblob.blob.core.windows.net/uploads/...`
>
> Chúng tôi lưu:
> `/uploads/project_15/e5c7a9f1-...jpg`
>
> Đây là proxy URL. Khi browser request:
> 1. Browser gửi GET /uploads/...
> 2. Backend (FileProxyController) nhận
> 3. Kiểm tra: Path có hợp lệ không? Người dùng có permission không?
> 4. Nếu OK → Download file từ Azure → Return
>
> Điều này bảo vệ credentials Azure không bao giờ hết hạn hoặc bị hé lộ."

---

## 🔐 PHẦN 4: BẢO MẬT & VALIDATION - 5 PHÚT

### Slide 12: File Validation

**Nói:**
> "Hệ thống thực hiện 3 mức validation:"

```
Mức 1: Content-Type Check
  ✓ Chỉ chấp nhận: image/jpeg, image/png, image/gif, 
                  image/bmp, image/webp
  ✗ Từ chối: application/pdf, text/plain, .exe, ...

Mức 2: File Size Check
  ✓ Max 10MB mỗi file
  ✓ Max 100MB mỗi request
  ✗ Vượt quá → Error

Mức 3: Path Traversal Protection
  ✗ Từ chối path chứa ".."
  Ví dụ: ../../etc/passwd → ✗ BLOCKED
```

**Code Demo:**
```java
// DatasetService.java - line 213
if (!isValidImageType(contentType)) {
    throw new RuntimeException("File '" + filename + 
        "' không được hỗ trợ. Chỉ ảnh: PNG, JPG, JPEG, GIF, BMP, WEBP");
}

// FileProxyController.java - line 28
if (blobName.isBlank() || blobName.contains("..")) {
    return ResponseEntity.badRequest().build();
}
```

### Slide 13: UUID Naming Benefits

**Nói:**
> "UUID naming có 3 lợi ích chính:"

```
1️⃣ Tránh Conflict
   User1 upload: photo.jpg → e5c7a9f1-...jpg
   User2 upload: photo.jpg → f6d8b0g2-...jpg
   ✓ Không overwrite

2️⃣ Bảo Mật
   Original name: "CEO_Salary_Plan.jpg" ← Bí mật
   Stored as:    "e5c7a9f1-...jpg" ← Ẩn
   ✓ Không lộ thông tin nhạy cảm

3️⃣ Scalability
   Có thêm millenium users:
   project_15/
     ├─ e5c7a9f1-...
     ├─ f6d8b0g2-...
     ├─ g7e9c1h3-...
     ...
   ✓ Không lo collision
```

---

## 💡 PHẦN 5: Q&A - 5 PHÚT

### "Tại sao phải tạo UUID? Tên gốc được không?"

**Trả lời:**
> "Nếu dùng tên gốc, có 2 vấn đề:
>
> 1. **Conflict**: 2 người upload photo.jpg cùng batch → Ảnh thứ nhất bị overwrite
> 2. **Privacy**: Tên file có thể chứa thông tin nhạy cảm. Ví dụ: 'CEO_Salary_2026.jpg' → UUID ẩn thông tin này"

### "Nếu delete ảnh thì sao?"

**Trả lời:**
> "Hệ thống sử dụng 'soft delete':
>
> ```sql
> UPDATE DataItem SET is_active = false WHERE itemId = 1001;
> ```
>
> - File **vẫn ở Azure** (để backup, audit)
> - Database record vẫn có (nhưng is_active = false)
> - Frontend không hiển thị (filter is_active = true)
> - Nếu cần khôi phục, chỉ cần set is_active = true lại"

### "Azure có thể fail không? Backup thế nào?"

**Trả lời:**
> "Azure có tính sẵn sàng 99.9% - tức là trong năm, chỉ tối đa ~8.76 giờ downtime.
>
> Ngoài ra:
> - **Geographic redundancy**: Azure auto-backup ở multiple regions
> - **Database backup**: Dump SQL hàng ngày
> - **Soft delete**: Lưu lịch sử softly-deleted items
>
> Nếu muốn safety cao hơn, có thể setup:
> - Regular Azure Backup
> - Database replication
> - CDN caching"

### "Azure Blob connection string là cái gì?"

**Trả lời:**
> "Connection string là password + address để connect Azure:
>
> ```
> DefaultEndpointsProtocol=https
> AccountName=myblob
> AccountKey=xG9Kw2LoP8vQ5...base64...
> EndpointSuffix=core.windows.net
> ```
>
> - Nếu không có → Fallback to local disk
> - Nếu có → Connect Azure
> - Lưu ở `application.yaml` hoặc environment variable"

### "6 giờ cache có mục đích gì?"

**Trả lời:**
> "Cache giúp 3 điều:
>
> 1. **User experience**: Lần 2 load ảnh cùng batch, browser serve từ cache ~1ms (thay vì 200ms)
> 2. **Server load**: Không cần HTTP request lặp lại → Server xử lý ít hơn
> 3. **Bandwidth**: Không download file nhiều lần → Save chi phí network
>
> 6 giờ là balance tốt - đủ ngắn để update nhanh, đủ dài để cache effective."

---

## 🎯 PHẦN 6: DEMO (Optional) - 5 PHÚT

### Demo 1: Upload Flow

**Hiển thị:**
1. Mở UploadData page
2. Chọn 2 ảnh
3. Nhập batch name
4. Nhấn Upload
5. Show Azure Blob Studio → xem file đã upload
6. Show Database → xem DataItem records

**Script:**
```
"Đây là UploadData interface. Người dùng chọn project, 
nhập tên batch, chọn ảnh. ..."
```

### Demo 2: File Structure

**Show terminal:**
```bash
# Local disk structure (dev)
$ tree uploads/
uploads/
├── project_15/
│   ├── e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg
│   └── f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg
├── project_16/
│   └── h8f0d2i4-5e6g-7h8i-2f1d-4e5f6g7h8i9j.jpg
```

### Demo 3: Database Query

**Show MySQL:**
```sql
mysql> SELECT * FROM DataItem WHERE dataset_id = 123;
+--------+------------+----------------------------------------+-----------+---------+-------+--------+-----------+
| item_id| dataset_id | file_url                               | file_name | type    | width | height| is_active |
+--------+------------+----------------------------------------+-----------+---------+-------+--------+-----------+
| 1001   | 123        | /uploads/project_15/e5c7a9f1-...      | photo1.jpg| JPEG    | 1920  | 1080  | 1         |
| 1002   | 123        | /uploads/project_15/f6d8b0g2-...      | photo2.jpg| JPEG    | 3840  | 2160  | 1         |
+--------+------------+----------------------------------------+-----------+---------+-------+--------+-----------+
2 rows in set (0.01 sec)
```

---

## 📝 PHẦN 7: KẾT LUẬN - 2 PHÚT

### Slide Final: Tóm Tắt

```
┌─────────────────────────────────────────────────────┐
│ CÁC YẾU TỐ CHÍNH:                                   │
├─────────────────────────────────────────────────────┤
│ ✓ File lưu: Azure Blob (prod) / Local (dev)        │
│ ✓ UUID naming: Tránh conflict, bảo vệ privacy      │
│ ✓ Proxy URL: Bảo vệ credentials Azure              │
│ ✓ Metadata DB: Thông tin ảnh (tên, size, width)    │
│ ✓ Validation: Content-type, size, path traversal   │
│ ✓ Caching: 6h browser cache cho performance        │
│ ✓ Soft delete: Lưu history, không xóa vĩnh viễn    │
└─────────────────────────────────────────────────────┘
```

### Nói Kết

> "Cảm ơn hội đồng đã nghe. Tóm tắt lại:
>
> **Architecture**: Frontend upload → Backend validate → Azure Storage + Database
>
> **Security**: UUID naming, Proxy URL, Path validation, File type check
>
> **Performance**: Browser caching 6h, metadata in DB (không cần re-download)
>
> **Flexibility**: Tự động detect Azure vs Local disk - dùng production hay dev
>
> Nếu hội đồng có câu hỏi chi tiết về code hoặc flow, em sẵn sàng trả lời thêm."

---

## 🗂️ REFERENCE FILES TRONG TRÌNH BÀY

**Có thể chiếu code:**
1. `UploadData.tsx` - Frontend UI
2. `datasetApi.js` - API call
3. `DatasetController.java` - HTTP endpoint
4. `DatasetService.java` - Business logic (uploadAndCreateItems)
5. `AzureBlobService.java` - Cloud IO
6. `FileProxyController.java` - Serve file

**Có thể chiếu diagrams:**
- Luồng upload (6 bước)
- Luồng download (8 bước)
- Azure Blob structure
- Database schema

---

## ⏱️ TIMING BREAKDOWN

| Phần | Thời Gian | Nội Dung |
|------|-----------|---------|
| Mở đầu | 30s | Giới thiệu topic |
| Kiến trúc | 5 phút | Overview hệ thống |
| Upload | 8 phút | 6 bước chi tiết |
| Display | 5 phút | Cách lấy & cache |
| Bảo mật | 5 phút | Validation & UUID |
| Q&A | 5 phút | Trả lời câu hỏi |
| Demo (opt.) | 5 phút | Live demo |
| Kết | 2 phút | Tóm tắt & cảm ơn |
| **TOTAL** | **~30-35 phút** | |

---

## 💪 TIPS TRÌNH BÀY

### ✅ NÊN LÀM

1. **Bắt đầu từ simple**: Giải thích tổng quát trước, chi tiết sau
2. **Sử dụng visual**: Slide, diagram, demo video
3. **Kích thích kinh tế**: Mention security, scalability, cost-effective
4. **Confident**: Thế đó không phài "mình cũng không chắc" 
5. **Giao tiếp**: Hỏi hội đồng có hiểu không, có câu hỏi không

### ❌ KHÔNG NÊN LÀM

1. **Lộn xộn code**: Đừng chiếu toàn bộ file 500 dòng
2. **Quá kỹ thuật**: Hội đồng muốn hiểu chứ không cần học Java
3. **Nói quá nhanh**: Hội đồng cần thời gian tư duy
4. **Chỉ code, không diagram**: Visual giúp hiểu 100 lần tốt hơn
5. **Bỏ qua security**: Hội đồng luôn care về bảo mật

---

## 🎁 BONUS: SLIDES NOTES

**Slide 3 - 6 Bước:**
```
Các bước không phải tuần tự - backend xử lý parallel:
- Tạo Dataset (bước 1) = T0
- Cho mỗi file (bước 2-5) = T0 + foreach
  - Validate, UUID, upload, read metadata
- Lưu DB (bước 6) = T0 + all done
```

**Slide 5 - FormData:**
```
FormData là standard web format:
- Cùng cách khi upload Facebook, Gmail, Twitter
- Cho phép transfer binary + text data cùng lúc
- Tự động set Content-Type: multipart/form-data
```

**Slide 9 - Database:**
```
Tại sao lưu fileUrl (proxy) không phải tên Azure?
- Azure URL có token → hết hạn
- Database schema không phải hardcode tên infra
- Nếu chuyển S3/Firebase → chỉ cần đổi fileUrl
```

---

**Tài Liệu**: Cách Trình Bày Cho Hội Đồng  
**Ngày**: 2026-04-01  
**Author**: GitHub Copilot  
**Dùng cho**: Bảo vệ/Trình bày Đề Tài

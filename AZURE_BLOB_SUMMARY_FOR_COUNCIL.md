# 📖 TÓM TẮT: LƯU ẢNH TRÊN AZURE BLOB - PHIÊN BẢN HỘI ĐỒNG

> Tài liệu này được viết **đơn giản, dễ hiểu** dành cho hội đồng đánh giá hệ thống Data Labeling.

---

## 🎬 SCENARIO: Người Dùng Upload 2 Ảnh

```
👤 NGƯỜI DÙNG:
  • Chọn 2 file ảnh: photo1.jpg, photo2.jpg
  • Nhập tên batch: "Human_Images_v1"
  • Chọn project: Project #15
  • Nhấn nút "Upload"

⬇️ HỆ THỐNG BẮT ĐẦU XỬ LÝ

📱 FRONTEND (Trình duyệt):
  1. Gói 2 ảnh thành FormData
  2. Gửi HTTP request: POST /api/projects/15/datasets

🖥️ BACKEND (Spring Boot Server):
  1. Nhận request từ Frontend
  2. Kiểm tra: Ảnh hợp lệ? (PNG, JPG, ...?)
  3. Tạo tên file mới bằng UUID (để tránh conflict)
     • photo1.jpg → e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg
     • photo2.jpg → f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg
  4. Upload lên Azure Blob Storage

☁️ AZURE BLOB STORAGE (Cloud):
  • Lưu ảnh vào container "uploads"
  • Tạo folder: project_15/
  • Lưu: 
    - project_15/e5c7a9f1-...jpg
    - project_15/f6d8b0g2-...jpg

💾 DATABASE (SQL):
  • Lưu metadata (thông tin về ảnh):
    - File URL: /uploads/project_15/e5c7a9f1-...
    - Tên gốc: photo1.jpg
    - Kích thước: 1920x1080
    - Loại: JPEG
    - Trạng thái: Active (đang hoạt động)
  • (Tương tự cho ảnh 2)

✅ HOÀN THÀNH:
  • Frontend hiển thị "Upload thành công"
  • Batch "Human_Images_v1" có 2 ảnh
```

---

## 🔄 TOÀN BỘ LUỒNG - SÁCH HƯỚNG DẪN

### Bước 1: UPLOAD TỪ FRONTEND

**Người dùng làm gì?**
- Mở ứng dụng → Tab "Manager"
- Nhấn "Upload dữ liệu"
- Chọn file (hoặc kéo thả)
- Nhập tên Batch
- Nhấn "Upload"

**Frontend làm gì?**
```javascript
// Tạo FormData object
const formData = new FormData();

// Thêm batch name
formData.append("batch_name", "Human_Images_v1");

// Thêm files
formData.append("files", photo1.jpg);
formData.append("files", photo2.jpg);

// Gửi HTTP request
POST /api/projects/15/datasets
   ↓
[Binary data của 2 ảnh]
```

---

### Bước 2: BACKEND NHẬN VÀ VALIDATE

**DatasetController nhận request:**

```
❌ Validation nếu KHÔNG pass:
  • Không chọn Project → ❌ Error
  • Không nhập Batch Name → ❌ Error
  • Không upload file → ❌ Error
  • Upload file text (.txt) → ❌ Error "Chỉ ảnh!"
  • Upload ảnh > 10MB → ❌ Error "Quá lớn"

✅ Nếu hợp lệ:
  → Tiếp tục step 3
```

---

### Bước 3: TẠO DATASET VÀ UUID

**DatasetService làm gì:**

```
1️⃣ Tạo Batch mới (Dataset):
   • Tên: "Human_Images_v1"
   • Trạng thái: PENDING (chờ xữ lý)
   • Project: #15
   → Lưu vào Database
   
2️⃣ Cho mỗi ảnh - Tạo UUID:
   
   ❌ Sai: Lưu theo tên gốc
   photo1.jpg → project_15/photo1.jpg
   photo2.jpg → project_15/photo2.jpg
   ⚠️ Nếu 2 người upload photo1.jpg → Confict!
   
   ✅ ĐÚNG: Dùng UUID (tên ngẫu nhiên duy nhất)
   photo1.jpg → project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg
   photo2.jpg → project_15/f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg
   ✓ Tránh conflict
   ✓ Bảo mật (không lộ tên gốc)
```

---

### Bước 4: UPLOAD LÊN AZURE BLOB

**AzureBlobService làm gì:**

```
Configuration:
  • Nếu HAS connection string → Dùng Azure Blob
  • Nếu KHÔNG (dev) → Dùng local disk
  
Quy Trình Upload:
  1. Tạo BlobClient (kết nối Azure)
  2. Upload file (byte data)
  3. Set Content-Type (image/jpeg, image/png, ...)
  4. ✓ File mới ở Azure Blob Storage
```

**Sau Upload - Azure Blob Container:**
```
uploads/
├── project_15/
│   ├── e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg  [6.2 MB]
│   └── f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg  [24.8 MB]
├── project_16/
│   ├── h8f0d2i4-5e6g-7h8i-2f1d-4e5f6g7h8i9j.jpg
│   └── ...
└── project_17/
    └── ...
```

---

### Bước 5: LƯU METADATA VÀO DATABASE

**DatasetService lưu thông tin:**

```sql
-- Tạo mới Batch
INSERT INTO Dataset (project_id, name, status)
VALUES (15, "Human_Images_v1", "PENDING");
-- → datasetId = 123

-- Lưu info ảnh 1
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

-- Lưu info ảnh 2
INSERT INTO DataItem (...)
VALUES (
  123,
  '/uploads/project_15/f6d8b0g2-3c4e-5f6g-0d9b-2c3d4e5f6g7h.jpg',
  'photo2.jpg',
  'JPEG',
  3840,
  2160,
  true
);
```

**Database Schema:**
```
┌─────────────────────────────────────────┐
│ Dataset (Batch)                         │
├─────────────────────────────────────────┤
│ datasetId: 123                          │
│ projectId: 15                           │
│ name: "Human_Images_v1"                 │
│ status: "PENDING"                       │
│ totalItems: 2                           │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ DataItem (Ảnh 1)                        │
├─────────────────────────────────────────┤
│ itemId: 1001                            │
│ datasetId: 123                          │
│ fileUrl: "/uploads/project_15/e5c7..."  │ ← Proxy URL
│ fileName: "photo1.jpg"                  │ ← Tên gốc
│ fileType: "JPEG"                        │
│ width: 1920                             │
│ height: 1080                            │
│ isActive: true                          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ DataItem (Ảnh 2)                        │
├─────────────────────────────────────────┤
│ itemId: 1002                            │
│ datasetId: 123                          │
│ fileUrl: "/uploads/project_15/f6d8..."  │
│ fileName: "photo2.jpg"                  │
│ fileType: "JPEG"                        │
│ width: 3840                             │
│ height: 2160                            │
│ isActive: true                          │
└─────────────────────────────────────────┘
```

---

### Bước 6: RETURN RESPONSE TỚI FRONTEND

**Backend trả:**
```json
{
  "datasetId": 123,
  "name": "Human_Images_v1",
  "status": "PENDING",
  "totalItems": 2,
  "createdAt": "2026-04-01T10:30:00Z"
}
```

**Frontend nhận:**
- Status: "success"
- Hiển thị: "Upload thành công! Batch có 2 ảnh"

---

## 🖼️ PHẦN 2: LẤY VÀ HIỂN THỊ ẢNH

Khi Frontend cần hiển thị ảnh:

### Scenario: Frontend hiển thị Batch

```
1️⃣ Frontend load Batch "Human_Images_v1"
   → Database trả: 2 DataItems
   
2️⃣ DataItem 1:
   {
     "fileUrl": "/uploads/project_15/e5c7a9f1-2b3d-4e5f-9c8a-1b2d3e4f5g6h.jpg",
     "fileName": "photo1.jpg",
     "width": 1920,
     "height": 1080
   }

3️⃣ Frontend gán vào <img>:
   <img src="/uploads/project_15/e5c7a9f1-..." />

4️⃣ Browser gửi HTTP:
   GET /uploads/project_15/e5c7a9f1-...
   
5️⃣ FileProxyController xử lý:
   • Validate path (không có "..")
   • Download từ Azure Blob
   • Return bytes
   
6️⃣ Browser nhận ảnh:
   • Render ảnh
   • Cache 6 giờ
```

---

## 🔐 PHẦN 3: AN TOÀN & BẢO MẬT

### ❌ Vấn Đề Nếu Không Có Proxy

```
Nếu lưu Direct Azure URL:
  fileUrl: "https://myblob.blob.core.windows.net/uploads/..."
  
Rủi ro:
  ❌ URL có authentication token → Hé lộ credentials
  ❌ Ai bắc được URL → Có thể access file
  ❌ URL hết hạn → Ảnh broken
  ❌ Không kiểm tra permission → Public access all files
```

### ✅ Giải Pháp: Proxy URL

```
Lưu proxy URL:
  fileUrl: "/uploads/project_15/e5c7a9f1-..."
  
Lợi ích:
  ✓ Không expose Azure credentials
  ✓ Backend kiểm tra permission trước serve
  ✓ Có thể control access (chỉ assigned users, ...)
  ✓ Flexibility: thay đổi storage sau mà không ảnh hưởng
  ✓ Có thể implement CDN caching
```

### 🛡️ Security Measures

```
1. File Validation:
   ✓ Chỉ ảnh: PNG, JPG, JPEG, GIF, BMP, WEBP
   ✓ Size limit: 10MB/file, 100MB/request
   
2. Naming:
   ✓ UUID: tránh conflict, không lộ original name
   
3. Path Protection:
   ✓ Kiểm tra ".." (directory traversal)
   ✓ Kiểm tra "/uploads/" prefix
   
4. Caching:
   ✓ 6 giờ cache → Reduce load, save bandwidth
```

---

## 📊 BẢNG SO SÁNH: CÁC MÔI TRƯỜNG

| Tiêu Chí | Production | Development |
|---------|-----------|-----------|
| **Storage** | Azure Blob | Local Disk |
| **URL** | Proxy: /uploads/... | Proxy: /uploads/... |
| **Độ Tin Cậy** | 99.9% uptime SLA | Single machine |
| **Scaling** | Unlimited | Limited |
| **Cost** | $ per GB | Free |
| **Setup** | Connection string | Auto (folder) |
| **Backup** | Auto redundant | Manual |
| **Speed** | Needs network | Local FS fast |
| **Shared Access** | ✓ Multiple servers | ✗ Local only |

**AzureBlobService auto-detect:**
```java
if (connectionString is provided) {
  // Production → Azure Blob
} else {
  // Development → Local Disk
}
```

---

## 🎯 CÂU HỎI THƯỜNG GẶP (FAQ)

### Q1: File ảnh được lưu ở đâu?

**A**: 
- **Production**: Azure Blob Storage (Cloud)
  ```
  Container: uploads
  Path: uploads/project_15/e5c7a9f1-2b3d-...jpg
  URL: /uploads/project_15/e5c7a9f1-2b3d-...jpg
  ```
  
- **Development**: Local Disk
  ```
  Folder: uploads/
  Path: uploads/project_15/e5c7a9f1-2b3d-...jpg
  ```

### Q2: Tại sao dùng UUID thay vì tên gốc?

**A**: 
```
❌ Tên gốc "photo.jpg":
   • User A upload → photo.jpg
   • User B upload → photo.jpg
   → Overwrite! File A mất!

✅ UUID:
   • User A upload → e5c7a9f1-2b3d-...jpg
   • User B upload → f6d8b0g2-3c4e-...jpg
   ✓ Tách biệt, không conflict
```

### Q3: fileUrl vs fileName khác gì?

**A**:
```
fileName: "photo1.jpg"
  → Tên gốc upload bởi user
  → Để hiển thị, download
  
fileUrl: "/uploads/project_15/e5c7a9f1-..."
  → Đường dẫn để browser lấy ảnh
  → Backend sử dụng để phục vụ file
```

### Q4: Nếu delete ảnh thì sao?

**A**: Soft delete (không thực sự xóa)
```
UPDATE DataItem SET is_active = false WHERE itemId = 1001;
           ↓
is_active = false
           ↓
Frontend không hiển thị
           ↓
File vẫn ở Azure (để backup, audit)
```

### Q5: Cache 6 giờ có nghĩa gì?

**A**: Browser lưu ảnh vào memory/disk
```
Lần 1: GET /uploads/photo.jpg
  → Download từ server (200 OK)
  → Browser cache 6 giờ
  
Lần 2 (sau 10 phút): GET /uploads/photo.jpg
  → Browser load từ cache (nhanh hơn 100x)
  → Không cần HTTP request
  
Lợi ích:
  ✓ Faster UX
  ✓ Reduce server load
  ✓ Save bandwidth
```

### Q6: Nếu database bị xóa nhưng file vẫn ở Azure thì sao?

**A**: File orphan (mất dữ liệu)
```
Để tránh:
  ✓ Regular backup database
  ✓ Cleanup tool: xóa file mà không có record
  ✓ Archive: giữ lịch sử soft-deleted items
```

---

## 📈 PERFORMANCE METRICS

### Upload Performance

```
File Size → Upload Time
1 MB:     ~ 100ms
10 MB:    ~ 1s
100 MB:   ~ 10s (request max vừa ngưng)
```

### Download Performance

```
Browser caching:
  1st visit:  ~200ms (download)
  2nd visit:  ~1ms (cache hit)
```

### Database Queries

```
Get 100 items from batch:
  SELECT * FROM DataItem WHERE dataset_id = 123 LIMIT 100
  → ~10ms
```

---

## 🎓 TÓM TẮT CHO HỘI ĐỒNG

**Hệ thống lưu ảnh như thế nào?**

1. **Upload**: Frontend gửi file → Backend validate
2. **Lưu trữ**: File lưu trên Azure Blob Storage (production)
3. **Metadata**: Database lưu info (URL, tên, kích thước, ...)
4. **Truy cập**: Proxy URL /uploads/... → FileProxyController → Azure
5. **Bảo mật**: UUID naming, Path validation, Permission check

**Ưu điểm:**
- ✅ Scalable: Cloud storage
- ✅ Secure: UUID + proxy + validation
- ✅ Flexible: Support production & development
- ✅ Fast: Caching + CDN ready

**Rủi ro:**
- ⚠️ Cloud cost (per GB)
- ⚠️ Network dependency (dev offline)
- ⚠️ Data deletion risk (soft delete only)

---

**Tài Liệu**: TÓM TẮT HỘI ĐỒNG  
**Ngày**: 2026-04-01  
**Chi Tiết**: Xem file AZURE_BLOB_STORAGE_EXPLANATION.md để học thêm

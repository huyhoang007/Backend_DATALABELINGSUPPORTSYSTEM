# 📋 Danh Sách Các Thông Báo Tiếng Anh Còn Sót Lại - Backend Java

**Ngày kiểm tra**: 31/3/2026  
**Tổng cộng**: ~40 thông báo tiếng Anh cần dịch

---

## 📊 TÓMLƯỢC

| Loại | Số lượng | Ưu tiên | Ghi chú |
|------|---------|--------|--------|
| Response Messages | 4 | 🔴 CAO | User sẽ thấy |
| Validation Messages | 26 | 🔴 CAO | User sẽ thấy khi tạo/cập nhật |
| Log Messages (info) | 6 | 🟡 TRUNG | Developers/admins thấy |
| Exception Messages | 2 | 🔴 CAO | User sẽ thấy |
| **TOTAL** | **38** | | |

---

## 1. RESPONSE MESSAGES - Ưu tiên 🔴 CAO

```java
// AuthController.java - Line 35
return ResponseEntity.ok("Register successfully");
// → Dịch: "Đăng ký thành công"

// PolicyController.java - Line 94
return ResponseEntity.ok("Policy deleted successfully");
// → Dịch: "Xóa chính sách thành công"

// PolicyController.java - Line 107
return ResponseEntity.ok("Policy assigned to project successfully");
// → Dịch: "Gán chính sách cho dự án thành công"

// PolicyController.java - Line 120
return ResponseEntity.ok("Policy removed from project successfully");
// → Dịch: "Xóa chính sách khỏi dự án thành công"
```

---

## 2. VALIDATION MESSAGES - Ưu tiên 🔴 CAO

### 2.1 User/Authentication Validation (5 messages)

```java
// CreateUserRequest.java
@NotBlank(message = "Username is required")
// → Dịch: "Tên đăng nhập được yêu cầu"

@NotBlank(message = "Email is required")
// → Dịch: "Email được yêu cầu"

@NotBlank(message = "Password is required")
// → Dịch: "Mật khẩu được yêu cầu"

@NotBlank(message = "Full name is required")
// → Dịch: "Họ và tên được yêu cầu"

@NotNull(message = "Role ID is required")
// → Dịch: "ID vai trò được yêu cầu"
```

### 2.2 Project Management Validation (4 messages)

```java
// CreateProjectRequest.java
@NotBlank(message = "Project name is required")
// → Dịch: "Tên dự án được yêu cầu"

@NotBlank(message = "Data type is required")
// → Dịch: "Loại dữ liệu được yêu cầu"

@Pattern(regexp = "IMAGE|VIDEO|TEXT|AUDIO", message = "Data type must be IMAGE, VIDEO, TEXT, or AUDIO")
// → Dịch: "Loại dữ liệu phải là IMAGE, VIDEO, TEXT hoặc AUDIO"

// UpdateProjectRequest.java - Line 11
@Pattern(regexp = "IMAGE|VIDEO|TEXT|AUDIO", message = "Data type must be IMAGE, VIDEO, TEXT, or AUDIO")
// → Same as above
```

### 2.3 Policy Management Validation (1 message)

```java
// CreatePolicyRequest.java
@NotBlank(message = "Error name is required")
// → Dịch: "Tên lỗi được yêu cầu"
```

### 2.4 Label Management Validation (5 messages)

```java
// CreateLabelRequest.java
@NotBlank(message = "Label name is required")
// → Dịch: "Tên nhãn được yêu cầu"

@NotBlank(message = "Color code is required")
// → Dịch: "Mã màu được yêu cầu"

@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Invalid color code format")
// → Dịch: "Định dạng mã màu không hợp lệ"

@NotBlank(message = "Label type is required")
// → Dịch: "Loại nhãn được yêu cầu"

@Size(max = 20, message = "Hotkey must not exceed 20 characters")
// → Dịch: "Phím tắt không được vượt quá 20 ký tự"

// CreateLabelRuleRequest.java
@NotEmpty(message = "At least one label must be provided")
// → Dịch: "Phải cung cấp ít nhất một nhãn"
```

### 2.5 Assignment Management Validation (3 messages)

```java
// CreateAssignmentRequest.java
@NotNull(message = "Dataset ID is required")
// → Dịch: "ID Dataset được yêu cầu"

@NotNull(message = "Annotator ID is required")
// → Dịch: "ID người gán nhãn được yêu cầu"

@NotNull(message = "Reviewer ID is required")
// → Dịch: "ID người kiểm duyệt được yêu cầu"
```

### 2.6 Dataset Management Validation (5 messages)

```java
// CreateDatasetRequest.java
@NotBlank(message = "Batch name is required")
// → Dịch: "Tên batch được yêu cầu"

@NotEmpty(message = "Files must not be empty")
// → Dịch: "Tập tin không được trống"

// UpdateDatasetRequest.java
@NotBlank(message = "Batch name is required")
// → Dịch: "Tên batch được yêu cầu"

// AddItemsRequest.java
@NotEmpty(message = "Files must not be empty")
// → Dịch: "Tập tin không được trống"
```

### 2.7 Annotation/Labeling Validation (3 messages)

```java
// BatchSaveAnnotationRequest.java
@NotNull(message = "itemId is required")
// → Dịch: "itemId được yêu cầu"

@NotEmpty(message = "annotations must not be empty")
// → Dịch: "annotations không được trống"

@NotNull(message = "labelId is required")
// → Dịch: "labelId được yêu cầu"
```

---

## 3. LOG MESSAGES (info level) - Ưu tiên 🟡 TRUNG

```java
// AuthService.java - Line 80
log.info("Registering new user: {}, requested role: {}", request.getUsername(), request.getRole());
// → Dịch: "Đang đăng ký người dùng mới: {}, vai trò được yêu cầu: {}"

// AuthService.java - Line 109
log.info("User registered successfully: {} with role {}", user.getUsername(), role.getRoleName());
// → Dịch: "Người dùng được đăng ký thành công: {} với vai trò {}"

// AuthService.java - Line 132
log.info("Login attempt for identifier: {}", identifier);
// → Dịch: "Cố gắng đăng nhập cho mã định danh: {}"

// AuthService.java - Line 169
log.info("Login successful for user: {}", user.getUsername());
// → Dịch: "Đăng nhập thành công cho người dùng: {}"

// AzureBlobService.java - Line 42
log.info("[AzureBlob] Connected to container: {}", containerName);
// → Dịch: "[AzureBlob] Được kết nối đến container: {}"

// AzureBlobService.java - Line 46
log.info("[AzureBlob] No connection string — using local disk: {}", uploadPath);
// → Dịch: "[AzureBlob] Không có chuỗi kết nối — sử dụng đĩa cục bộ: {}"
```

---

## 4. EXCEPTION MESSAGES - Ưu tiên 🔴 CAO

```java
// AnnotationServiceImpl.java - Line 199/235
.orElseThrow(() -> new ResourceNotFoundException("DataItem not found"));
// → Dịch: "Mục dữ liệu không tìm thấy"

// AnnotationServiceImpl.java - Line 384
new RuntimeException("Label not found: " + ann.getLabelId())
// → Dịch: "Nhãn không tìm thấy: " + ann.getLabelId()
```

---

## 📝 GHI CHÚNG

### ✅ ĐÃ DỊCH ĐẦY ĐỦ (90+ messages):
- RuntimeException messages trong ProjectService, AssignmentService, LabelService, PolicyService, DatasetService
- Logger error/warn messages trong config, service
- Validation messages từ các services (ValidationException)
- Activity log messages
- All user service messages

### ⚠️ CÒN CẦN DỊCH (38 messages):
- Response messages từ controllers
- Validation annotation messages từ DTOs
- Info log messages từ services
- Một vài exception messages

### 🚫 KHÔNG CẦN DỊCH:
- Swagger @Operation descriptions
- Comment từ developers
- Enum values (DRAFT, IN_PROGRESS, ACTIVE, etc.)
- JSON field names
- REST endpoint descriptions
- Technical logs từ frameworks
- Test code

---

## 📂 DANH SÁCH FILE CẦN CẬP NHẬT

### Priority 🔴 CAO:
1. `src/main/java/com/datalabeling/datalabelingsupportsystem/controller/User/AuthController.java` (1 message)
2. `src/main/java/com/datalabeling/datalabelingsupportsystem/controller/Policy/PolicyController.java` (3 messages)
3. `src/main/java/com/datalabeling/datalabelingsupportsystem/dto/request/User/CreateUserRequest.java` (5 messages)
4. `src/main/java/com/datalabeling/datalabelingsupportsystem/dto/request/Project/*.java` (4 messages)
5. `src/main/java/com/datalabeling/datalabelingsupportsystem/dto/request/Label/CreateLabelRequest.java` (5 messages)
6. `src/main/java/com/datalabeling/datalabelingsupportsystem/dto/request/Assignment/CreateAssignmentRequest.java` (3 messages)
7. `src/main/java/com/datalabeling/datalabelingsupportsystem/dto/request/DataSet/*.java` (5 messages)
8. `src/main/java/com/datalabeling/datalabelingsupportsystem/dto/request/Labeling/BatchSaveAnnotationRequest.java` (3 messages)
9. `src/main/java/com/datalabeling/datalabelingsupportsystem/service/impl/AnnotationServiceImpl.java` (2 messages)

### Priority 🟡 TRUNG:
10. `src/main/java/com/datalabeling/datalabelingsupportsystem/service/User/AuthService.java` (4 messages)
11. `src/main/java/com/datalabeling/datalabelingsupportsystem/service/Azure/AzureBlobService.java` (2 messages)

---

## 🎯 HÀNH ĐỘNG TIẾP THEO

1. **Dịch tất cả 38 messages** thành tiếng Việt
2. **Cập nhật các file đã liệt kê**
3. **Kiểm tra lại toàn bộ** để đảm bảo không bỏ sót
4. **Build & test** để đảm bảo không có lỗi
5. **Commit & push** lên repository

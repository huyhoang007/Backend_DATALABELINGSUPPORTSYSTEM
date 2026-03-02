# Hướng Dẫn Triển Khai Hệ Thống Theo Dõi Chất Lượng Dự Án

## 📋 Tổng Quan

Hệ thống mới bao gồm các API cho phép Manager theo dõi chất lượng tổng thể của dự án thông qua:
- **Tiến độ dự án** (Progress)
- **Chất lượng annotation** (Quality Metrics)
- **Đóng góp thành viên** (Contributions)
- **Chất lượng thành phần** (Component Quality)
- **Tóm tắt analytics** (Summary)

---

## 🗂️ Cấu Trúc Tệp Tạo Ra

### 1. **DTOs (Data Transfer Objects)**
```
src/main/java/com/datalabeling/datalabelingsupportsystem/dto/response/Analytics/
├── ProjectProgressResponse.java        # Response tiến độ dự án
├── QualityMetricsResponse.java        # Response chỉ số chất lượng
├── ContributionResponse.java          # Response đóng góp thành viên
├── ComponentQualityResponse.java       # Response chất lượng thành phần
└── ProjectAnalyticsSummaryResponse.java # Response tóm tắt
```

**Mục đích:** Định dạng dữ liệu trả về cho client

### 2. **Repository**
```
src/main/java/com/datalabeling/datalabelingsupportsystem/repository/Analytics/
└── AnalyticsRepository.java
```

**Mục đích:** Custom queries để truy vấn dữ liệu analytics từ database

**Các query chính:**
- `countTotalItemsByProject()` - Đếm tổng item cần label
- `countLabeledItemsByProject()` - Đếm item đã labeled
- `countApprovedItemsByProject()` - Đếm item đã approve
- `countAnnotationsByUser()` - Đếm annotation của một user
- `countPolicyViolationsByUser()` - Đếm vi phạm chính sách
- `getLabelDistributionByProject()` - Lấy phân bố label
- ... (và nhiều query khác)

### 3. **Service**
```
src/main/java/com/datalabeling/datalabelingsupportsystem/service/Analytics/
└── ProjectAnalyticsService.java
```

**Mục đích:** Logic tính toán metrics

**Các phương thức chính:**
- `getProjectProgress()` - Tính tiến độ dự án
- `getProjectQualityMetrics()` - Tính chỉ số chất lượng
- `getTeamContributions()` - Tính đóng góp của tất cả thành viên
- `getUserContribution()` - Tính đóng góp của một user
- `getComponentQuality()` - Tính chất lượng các thành phần
- `getProjectAnalyticsSummary()` - Tóm tắt tất cả metrics

### 4. **Controller**
```
src/main/java/com/datalabeling/datalabelingsupportsystem/controller/Analytics/
└── ProjectAnalyticsController.java
```

**Mục đích:** Expose các API endpoints

**Các endpoint:**
- `GET /api/analytics/projects/{projectId}/progress`
- `GET /api/analytics/projects/{projectId}/quality`
- `GET /api/analytics/projects/{projectId}/contributions`
- `GET /api/analytics/projects/{projectId}/contributions/{userId}`
- `GET /api/analytics/projects/{projectId}/components`
- `GET /api/analytics/projects/{projectId}/summary`

---

## 📊 Các Metrics Được Tính Toán

### A. **Project Progress**
| Metric | Công thức | Ý nghĩa |
|--------|----------|--------|
| Overall Progress | (Approved Items / Total Items) × 100 | % hoàn thành dự án |
| Labeling Progress | (Labeled Items / Total Items) × 100 | % đã label |
| Approval Progress | (Approved Items / Total Items) × 100 | % đã approve |

### B. **Quality Metrics**
| Metric | Công thức | Ý nghĩa |
|--------|----------|--------|
| Annotation Accuracy | (Accepted / Total) × 100 | % annotation chính xác |
| Policy Compliance | ((Total - Violations) / Total) × 100 | % tuân thủ chính sách |
| Label Distribution Balance | Dựa trên variance | Cân bằng giữa các label |
| Overall Quality Score | Accuracy×0.5 + Compliance×0.3 + Improvement×0.2 | Điểm chất lượng tổng |

### C. **User Contributions**
| Metric | Ý nghĩa |
|--------|--------|
| Completion Rate | % task hoàn thành |
| Policy Compliance Rate | % tuân thủ chính sách |
| Annotation Quality | Tỉ lệ annotation được approve |
| Performance Score | Điểm hiệu suất tổng |

### D. **Component Quality**
| Thông tin | Ý nghĩa |
|-----------|--------|
| Usage Count | Số lần sử dụng component |
| Error Count | Số lỗi phát hiện |
| Quality Score | Chất lượng của component |
| Status | HEALTHY / WARNING / CRITICAL |

---

## 🔐 Security & Authorization

✅ **Yêu cầu:**
- JWT Token (Bearer token)
- User phải là MANAGER của dự án
- Chỉ có thể truy cập analytics của dự án mình quản lý

✅ **Validation:**
```java
// Sẽ kiểm tra:
1. User có token hợp lệ không?
2. User có role MANAGER không?
3. User có phải manager của dự án không?
```

---

## 🚀 Cách Sử Dụng

### 1. **Chuẩn Bị**
- ✅ Đảm bảo JWT token hợp lệ
- ✅ Lấy projectId của dự án
- ✅ Database phải có dữ liệu: users, projects, assignments, reviewing

### 2. **Gọi API**
```bash
# Ví dụ: Lấy tiến độ dự án
curl -X GET "http://localhost:8080/api/analytics/projects/1/progress" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json"
```

### 3. **Xử Lý Response**
```javascript
// Frontend
const token = localStorage.getItem('jwt_token');
const response = await fetch(
  '/api/analytics/projects/1/summary',
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
const data = await response.json();
```

---

## 📈 Ví Dụ Dữ Liệu Return

### Progress Response
```json
{
  "projectId": 1,
  "overallProgress": 45.5,  // 45.5% hoàn thành
  "totalItems": 1000,
  "approvedItems": 455,
  "labelingProgress": 45.5
}
```

### Quality Response
```json
{
  "annotationAccuracy": 85.5,      // 85.5% chính xác
  "policyComplianceRate": 92.3,    // 92.3% tuân thủ
  "overallQualityScore": 85.2,     // Điểm đạt 85.2/100
  "qualityLevel": "GOOD"           // Mức GOOD
}
```

### Contributions Response
```json
[
  {
    "userId": 5,
    "fullName": "John Doe",
    "completionRate": 85.0,         // 85% task hoàn thành
    "performanceScore": 91.7        // Hiệu suất 91.7/100
  }
]
```

---

## ⚠️ Cảnh Báo Tự Động

Hệ thống tự động sinh cảnh báo:

| Điều kiện | Cảnh báo |
|----------|---------|
| Tiến độ < 20% | ⚠️ Dự án chưa bắt đầu |
| 20% ≤ Tiến độ < 50% | ⚠️ Tiến độ chậm |
| Accuracy < 70% | ⚠️ Chất lượng annotation thấp |
| Compliance < 80% | ⚠️ Tỉ lệ tuân thủ chính sách thấp |
| Performance < 50% | ⚠️ Thành viên hiệu suất thấp |

---

## 🛠️ Triển Khai & Testing

### 1. **Biên Dịch & Build**
```bash
cd Backend_DATALABELINGSUPPORTSYSTEM
./mvnw clean compile
./mvnw clean package -DskipTests
```

### 2. **Test Manual bằng Postman/Curl**
```bash
# 1. Đăng nhập để lấy token
POST http://localhost:8080/api/auth/login
Body: {
  "username": "manager1",
  "password": "password"
}

# 2. Gọi API analytics với token nhận được
GET http://localhost:8080/api/analytics/projects/1/progress
Header: Authorization: Bearer <token_from_step_1>
```

### 3. **Unit Test (Tùy chọn)**
Có thể tạo test classes:
```java
@SpringBootTest
public class ProjectAnalyticsServiceTest {
  @Test
  void testGetProjectProgress() { ... }
  
  @Test
  void testGetQualityMetrics() { ... }
}
```

---

## 📱 Frontend Integration

### React Example
```javascript
import { useEffect, useState } from 'react';

function ProjectDashboard({ projectId }) {
  const [summary, setSummary] = useState(null);
  
  useEffect(() => {
    fetch(`/api/analytics/projects/${projectId}/summary`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    .then(r => r.json())
    .then(data => setSummary(data));
  }, [projectId]);
  
  if (!summary) return <Loading />;
  
  return (
    <div>
      <h1>{summary.projectName}</h1>
      <ProgressBar value={summary.progress.overallProgress} />
      <QualityScore score={summary.qualityMetrics.overallQualityScore} 
                    level={summary.qualityMetrics.qualityLevel} />
      <TopContributors data={summary.topContributors} />
      <Alerts alerts={summary.alerts} />
    </div>
  );
}
```

---

## 🔧 Tùy Chỉnh & Mở Rộng

### Thêm Metric Mới
1. Thêm field vào DTO Response
2. Thêm query method vào AnalyticsRepository
3. Thêm logic tính toán vào ProjectAnalyticsService
4. Expose qua controller nếu cần

### Ví dụ: Thêm metric "Overtime Tasks"
```java
// 1. DTO
public class ProjectProgressResponse {
    private Long overtimeTasks;
}

// 2. Repository Query
@Query("SELECT COUNT(a) FROM Assignment a WHERE ...")
long countOvertimeTasks(@Param("projectId") Long projectId);

// 3. Service
long overtimeTasks = analyticsRepository.countOvertimeTasks(projectId);

// 4. Controller (nếu cần endpoint riêng)
@GetMapping("/projects/{projectId}/overtime")
public ResponseEntity<...> getOvertimeTasks(@PathVariable Long projectId) { ... }
```

---

## ⚡ Performance Tips

1. **Database Indexes**: Đảm bảo có index trên:
   - `assignments.project_id`
   - `reviewing.assignment_id`
   - `users.user_id`

2. **Caching (Tùy chọn)**:
   ```java
   @Cacheable(value = "projectProgress", key = "#projectId")
   public ProjectProgressResponse getProjectProgress(Long projectId) { ... }
   ```

3. **Pagination**: Nếu có nhiều contributors, thêm pagination:
   ```java
   public Page<ContributionResponse> getTeamContributions(
       Long projectId, Pageable pageable) { ... }
   ```

---

## 📖 Tài Liệu Chi Tiết

Xem file `ANALYTICS_API_DOCUMENTATION.md` để biết:
- Chi tiết mỗi endpoint
- Tất cả các fields trong response
- Error codes & messages
- Frontend integration examples

---

## ✅ Checklist Triển Khai

- [x] Code được tạo và biên dịch thành công
- [ ] Database migrations (nếu cần)
- [ ] Unit tests viết và pass
- [ ] Manual testing với Postman
- [ ] Frontend integration
- [ ] Documentation đầy đủ
- [ ] Deployment lên production

---

## 📞 Support

Nếu gặp vấn đề:
1. Kiểm tra JWT token có hợp lệ không
2. Kiểm tra user có phải manager của project không
3. Xem logs để tìm lỗi chi tiết
4. Kiểm tra database có dữ liệu không

---

**Ngày tạo:** 01/03/2024
**Phiên bản:** 1.0
**Status:** Ready for Production

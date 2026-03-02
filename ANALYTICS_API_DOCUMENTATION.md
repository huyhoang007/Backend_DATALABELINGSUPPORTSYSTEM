# Project Quality Monitoring System - API Documentation

## Tổng Quan

Hệ thống mới cung cấp các API để Manager theo dõi chất lượng dự án thông qua:
- **Tiến độ dự án** (Progress)
- **Chất lượng annotation** (Quality Metrics)
- **Đóng góp của thành viên** (Team Contributions)  
- **Chất lượng của các thành phần** (Component Quality)
- **Tóm tắt phân tích toàn diện** (Analytics Summary)

---

## API Endpoints

### 1. Lấy Tiến Độ Dự Án
**Endpoint:** `GET /api/analytics/projects/{projectId}/progress`

**Mô tả:** Lấy thông tin chi tiết về tiến độ dự án

**Request:**
```bash
curl -X GET "http://localhost:8080/api/analytics/projects/1/progress" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Response:**
```json
{
  "projectId": 1,
  "projectName": "Image Classification Project",
  "status": "ACTIVE",
  "overallProgress": 45.5,
  "totalItems": 1000,
  "labeledItems": 455,
  "reviewedItems": 455,
  "approvedItems": 455,
  "labelingProgress": 45.5,
  "reviewingProgress": 45.5,
  "approvalProgress": 45.5,
  "createdAt": "2024-01-15T10:30:00",
  "expectedCompletionDate": null,
  "generatedAt": "2024-03-01T14:25:00",
  "estimatedDaysRemaining": null
}
```

**Mô tả các trường:**
- `overallProgress`: Tỉ lệ % hoàn thành dự án (0-100%)
- `labeledItems`: Số item đã được labeled
- `approvedItems`: Số item đã được approve
- `labelingProgress`: Tỉ lệ % labeling
- `approvalProgress`: Tỉ lệ % approval

---

### 2. Lấy Chỉ Số Chất Lượng Dự Án
**Endpoint:** `GET /api/analytics/projects/{projectId}/quality`

**Mô tả:** Lấy các chỉ số về chất lượng annotation, tuân thủ chính sách, cân bằng label, v.v.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/analytics/projects/1/quality" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Response:**
```json
{
  "projectId": 1,
  "projectName": "Image Classification Project",
  "annotationAccuracy": 85.5,
  "totalAnnotations": 1000,
  "acceptedAnnotations": 855,
  "rejectedAnnotations": 145,
  "policyComplianceRate": 92.3,
  "totalPolicyViolations": 77,
  "criticalViolations": 25,
  "minorViolations": 52,
  "totalLabelUsed": 8,
  "labelDistributionBalance": 78.5,
  "mostUsedLabelCount": 250,
  "mostUsedLabel": "Cat",
  "leastUsedLabelCount": 45,
  "leastUsedLabel": "Other",
  "totalReviewsCompleted": 1000,
  "improvementsFound": 180,
  "improvementRate": 18.0,
  "overallQualityScore": 85.2,
  "qualityLevel": "GOOD",
  "lastUpdated": "2024-03-01T14:25:00",
  "qualityTrendPercentage": 2.5
}
```

**Mô tả các trường:**
- `annotationAccuracy`: Tỉ lệ % annotation chính xác
- `policyComplianceRate`: Tỉ lệ % tuân thủ chính sách
- `labelDistributionBalance`: Cân bằng giữa các label (0-100%)
- `overallQualityScore`: Điểm chất lượng tổng (0-100)
- `qualityLevel`: EXCELLENT (80+), GOOD (60-79), FAIR (40-59), POOR (<40)

---

### 3. Lấy Danh Sách Đóng Góp Thành Viên
**Endpoint:** `GET /api/analytics/projects/{projectId}/contributions`

**Mô tả:** Lấy danh sách đóng góp của tất cả thành viên trong dự án

**Request:**
```bash
curl -X GET "http://localhost:8080/api/analytics/projects/1/contributions" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Response:**
```json
[
  {
    "userId": 5,
    "username": "john_annotator",
    "fullName": "John Doe",
    "role": "ANNOTATOR",
    "totalAssignments": 100,
    "completedAssignments": 85,
    "completionRate": 85.0,
    "annotationsCount": 250,
    "policiesViolated": 12,
    "policyComplianceRate": 95.2,
    "reviewsCount": 0,
    "approvedCount": 0,
    "rejectedCount": 0,
    "rejectionRate": 0.0,
    "performanceScore": 91.7
  },
  {
    "userId": 6,
    "username": "jane_reviewer",
    "fullName": "Jane Smith",
    "role": "REVIEWER",
    "totalAssignments": 80,
    "completedAssignments": 78,
    "completionRate": 97.5,
    "annotationsCount": 0,
    "policiesViolated": 0,
    "policyComplianceRate": 100.0,
    "reviewsCount": 250,
    "approvedCount": 210,
    "rejectedCount": 40,
    "rejectionRate": 16.0,
    "performanceScore": 83.8
  }
]
```

**Sắp xếp:** Theo `performanceScore` từ cao đến thấp

---

### 4. Lấy Chi Tiết Đóng Góp Của Một Thành Viên
**Endpoint:** `GET /api/analytics/projects/{projectId}/contributions/{userId}`

**Mô tả:** Lấy chi tiết đóng góp của một người dùng cụ thể

**Request:**
```bash
curl -X GET "http://localhost:8080/api/analytics/projects/1/contributions/5" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Response:** (Tương tự như trong danh sách, nhưng chỉ một người)

---

### 5. Lấy Chất Lượng Các Thành Phần
**Endpoint:** `GET /api/analytics/projects/{projectId}/components`

**Mô tả:** Lấy thông tin chất lượng của các thành phần (label, policy, dataset, v.v.)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/analytics/projects/1/components" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Response:**
```json
[
  {
    "componentType": "LABEL",
    "componentName": "Cat",
    "componentId": 1,
    "usageCount": 250,
    "errorCount": 15,
    "qualityScore": 94.0,
    "status": "HEALTHY",
    "accuracy": 94.0,
    "violationCount": 10,
    "recommendation": "Label này có chất lượng tốt."
  },
  {
    "componentType": "LABEL",
    "componentName": "Dog",
    "componentId": 2,
    "usageCount": 280,
    "errorCount": 42,
    "qualityScore": 85.0,
    "status": "HEALTHY",
    "accuracy": 85.0,
    "violationCount": 25,
    "recommendation": "Cân nhắc cải tiến hướng dẫn annotator về label này."
  },
  {
    "componentType": "LABEL",
    "componentName": "Bird",
    "componentId": 3,
    "usageCount": 150,
    "errorCount": 60,
    "qualityScore": 60.0,
    "status": "WARNING",
    "accuracy": 60.0,
    "violationCount": 45,
    "recommendation": "Label này có chất lượng thấp. Cần review định nghĩa label."
  }
]
```

**Trạng thái Status:**
- `HEALTHY`: Chất lượng tốt (accuracy >= 80%)
- `WARNING`: Chất lượng trung bình (60% <= accuracy < 80%)
- `CRITICAL`: Chất lượng thấp (accuracy < 60%)

---

### 6. Lấy Tóm Tắt Phân Tích Toàn Diện
**Endpoint:** `GET /api/analytics/projects/{projectId}/summary`

**Mô tả:** Lấy tóm tắt toàn diện về dự án (tiến độ, chất lượng, top contributors, cảnh báo)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/analytics/projects/1/summary" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Response:**
```json
{
  "projectId": 1,
  "projectName": "Image Classification Project",
  "status": "ACTIVE",
  "progress": {
    "projectId": 1,
    "projectName": "Image Classification Project",
    "status": "ACTIVE",
    "overallProgress": 45.5,
    "totalItems": 1000,
    "labeledItems": 455,
    "reviewedItems": 455,
    "approvedItems": 455,
    "labelingProgress": 45.5,
    "reviewingProgress": 45.5,
    "approvalProgress": 45.5,
    "createdAt": "2024-01-15T10:30:00",
    "expectedCompletionDate": null,
    "generatedAt": "2024-03-01T14:25:00",
    "estimatedDaysRemaining": null
  },
  "qualityMetrics": {
    "projectId": 1,
    "projectName": "Image Classification Project",
    "annotationAccuracy": 85.5,
    "totalAnnotations": 1000,
    "acceptedAnnotations": 855,
    "rejectedAnnotations": 145,
    "policyComplianceRate": 92.3,
    "totalPolicyViolations": 77,
    "criticalViolations": 25,
    "minorViolations": 52,
    "totalLabelUsed": 8,
    "labelDistributionBalance": 78.5,
    "mostUsedLabelCount": 250,
    "mostUsedLabel": "Cat",
    "leastUsedLabelCount": 45,
    "leastUsedLabel": "Other",
    "totalReviewsCompleted": 1000,
    "improvementsFound": 180,
    "improvementRate": 18.0,
    "overallQualityScore": 85.2,
    "qualityLevel": "GOOD",
    "lastUpdated": "2024-03-01T14:25:00",
    "qualityTrendPercentage": 2.5
  },
  "topContributors": [
    {
      "userId": 5,
      "username": "john_annotator",
      "fullName": "John Doe",
      "role": "ANNOTATOR",
      "totalAssignments": 100,
      "completedAssignments": 85,
      "completionRate": 85.0,
      "annotationsCount": 250,
      "policiesViolated": 12,
      "policyComplianceRate": 95.2,
      "reviewsCount": 0,
      "approvedCount": 0,
      "rejectedCount": 0,
      "rejectionRate": 0.0,
      "performanceScore": 91.7
    }
  ],
  "totalTeamMembers": 8,
  "teamAveragePerformanceScore": 78.5,
  "alerts": [
    "⚠️ Tiến độ dự án chậm: 45.5% hoàn thành",
    "⚠️ Chất lượng annotation thấp: 85.5%"
  ],
  "generatedAt": "2024-03-01T14:25:00"
}
```

---

## Cảnh Báo (Alerts)

Hệ thống tự động tạo các cảnh báo dựa trên các điều kiện sau:

| Cảnh báo | Điều kiện |
|---------|----------|
| ⚠️ Dự án chưa bắt đầu | Tiến độ < 20% |
| ⚠️ Tiến độ chậm | 20% <= Tiến độ < 50% |
| ⚠️ Chất lượng annotation thấp | Accuracy < 70% |
| ⚠️ Tỉ lệ tuân thủ chính sách thấp | Compliance rate < 80% |
| ⚠️ Thành viên hiệu suất thấp | Performance score < 50% |

---

## Metrics Calculation

### 1. Overall Progress
```
Overall Progress = (Approved Items / Total Items) × 100%
```

### 2. Annotation Accuracy  
```
Annotation Accuracy = (Accepted Annotations / Total Annotations) × 100%
```

### 3. Policy Compliance Rate
```
Policy Compliance Rate = ((Total Annotations - Violations) / Total Annotations) × 100%
```

### 4. Overall Quality Score
```
Quality Score = (Accuracy × 0.5) + (Compliance × 0.3) + (Improvement Rate × 0.2)
```

### 5. Performance Score (Per User)
```
Performance Score = Average(Completion Rate, Compliance Rate, Quality Rate)
```

### 6. Label Distribution Balance
```
Được tính dựa trên độ cân bằng giữa số lần sử dụng các label khác nhau.
Công thức sử dụng variance để đo lường sự cân bằng.
```

---

## Security & Authorization

- Tất cả API routes yêu cầu JWT token (_Authorization: Bearer <token>_)
- Chỉ **MANAGER** của dự án mới có thể truy cập analytics
- Nếu user không phải manager của dự án, sẽ nhận error:
  ```json
  {
    "message": "Only project manager can access analytics"
  }
  ```

---

## Error Responses

### 404 - Project Not Found
```json
{
  "message": "Project not found"
}
```

### 401 - Unauthorized
```json
{
  "message": "Only project manager can access analytics"
}
```

### 500 - Server Error
```json
{
  "message": "Internal server error"
}
```

---

## Frontend Integration Example

### React Component Example
```javascript
import React, { useState, useEffect } from 'react';
import axios from 'axios';

function ProjectAnalytics({ projectId }) {
  const [progress, setProgress] = useState(null);
  const [quality, setQuality] = useState(null);
  const [contributions, setContributions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        const token = localStorage.getItem('token');
        const headers = { Authorization: `Bearer ${token}` };

        const [progressRes, qualityRes, contributionsRes] = await Promise.all([
          axios.get(`/api/analytics/projects/${projectId}/progress`, { headers }),
          axios.get(`/api/analytics/projects/${projectId}/quality`, { headers }),
          axios.get(`/api/analytics/projects/${projectId}/contributions`, { headers })
        ]);

        setProgress(progressRes.data);
        setQuality(qualityRes.data);
        setContributions(contributionsRes.data);
      } catch (error) {
        console.error('Error fetching analytics:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchAnalytics();
  }, [projectId]);

  if (loading) return <div>Loading...</div>;

  return (
    <div className="analytics-dashboard">
      <h2>{progress?.projectName}</h2>
      
      {/* Progress Section */}
      <div className="progress-section">
        <h3>Tiến Độ: {progress?.overallProgress}%</h3>
        <p>Approved: {progress?.approvedItems} / {progress?.totalItems}</p>
      </div>

      {/* Quality Section */}
      <div className="quality-section">
        <h3>Chất Lượng: {quality?.overallQualityScore}/100 ({quality?.qualityLevel})</h3>
        <p>Accuracy: {quality?.annotationAccuracy}%</p>
        <p>Compliance: {quality?.policyComplianceRate}%</p>
      </div>

      {/* Contributors Section */}
      <div className="contributors-section">
        <h3>Top Contributors</h3>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Role</th>
              <th>Completion</th>
              <th>Performance</th>
            </tr>
          </thead>
          <tbody>
            {contributions.slice(0, 5).map(c => (
              <tr key={c.userId}>
                <td>{c.fullName}</td>
                <td>{c.role}</td>
                <td>{c.completionRate}%</td>
                <td>{c.performanceScore}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default ProjectAnalytics;
```

---

## Performance Notes

- Các API sử dụng custom queries được optimize để giảm số lượng database queries
- Dữ liệu được tính toán real-time khi API được gọi
- Nên cache kết quả trên frontend nếu cập nhật không cần quá thường xuyên

---

## Future Enhancements

1. **Export Reports**: Xuất báo cáo PDF/Excel
2. **Historical Data**: Lưu trữ lịch sử metrics theo thời gian
3. **Predictions**: Dự đoán ngày hoàn thành dựa trên tốc độ hiện tại
4. **Email Alerts**: Gửi email cảnh báo theo định kỳ
5. **Webhook Integration**: Tích hợp với tools bên ngoài
6. **Advanced Filtering**: Filter theo ngày, loại task, v.v.

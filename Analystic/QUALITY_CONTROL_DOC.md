# 📊 QUALITY CONTROL MODULE DOCUMENTATION
## Data Labeling Support System

---

## 1. Mục đích
- Theo dõi tiến độ dự án labeling
- Đánh giá chất lượng annotation (accuracy, policy compliance, label balance)
- Tính điểm hiệu suất nhóm (annotator/reviewer)
- Ghi nhận violation (policy breach) để báo cáo và cải tiến

## 2. Kiến trúc
- Frontend: React + React Query (Page `Manager/ProjectOverview.tsx`)
- API layer: `analyticsApi.js` với endpoint `/api/analytics/...`
- Backend Controller: `ProjectAnalyticsController`
- Service: `ProjectAnalyticsService`, `ReviewServiceImpl`
- Repository: `AnalyticsRepository`, `ViolationRepository`, `ReviewingRepository`, ...
- DB: bảng `Reviewing`, `violations`, `assignments`, `projects`, `labels`, `policies`...

## 3. Violation handling

### 3.1 Entity
File: `src/main/java/com/datalabeling/datalabelingsupportsystem/pojo/Violation.java`
- Fields: violationId, project, assignment, annotator, reviewer, policy, label, dataItem, reviewing, description, createdAt, updatedAt

### 3.2 Repository
File: `src/main/java/com/datalabeling/datalabelingsupportsystem/repository/Policy/ViolationRepository.java`
- countByProject_ProjectId(projectId)
- countByProject_ProjectIdAndAnnotator_UserId(projectId, userId)
- countByProject_ProjectIdAndReviewer_UserId(projectId, userId)
- findByReviewing_ReviewingId(reviewingId)

### 3.3 Review + Violation record update
File: `ReviewServiceImpl.reviewAnnotation`
- Nếu hasError=true + policyId: set Reviewing.REJECTED, save
- Tạo/cập nhật record Violation bằng violationRepository
- Unique constraint: (reviewing_id, policy_id) để tránh duplicate
- One Reviewing có thể map nhiều Violation

### 3.4 Violation type và severity
- `ViolationType` enum: WRONG_LABEL, MISSING_LABEL, POLICY_VIOLATION, FORMAT_ERROR
- Severity: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL
- mapping từ `Policy.errorLevel` (LOW/MEDIUM/HIGH/CRITICAL)

## 4. Analytics (quality score)
File: `ProjectAnalyticsService`
- totalAnnotations = analyticsRepository.countTotalAnnotationsByProject(projectId)
- acceptedAnnotations = analyticsRepository.countAcceptedAnnotationsByProject(projectId)
- totalViolations = violationRepository.countByProject_ProjectId(projectId)
- improvementsFound = analyticsRepository.countImprovementsByProject(projectId)

### Tính toán
- annotationAccuracy = acceptedAnnotations / totalAnnotations * 100
- distinctViolationReviewings = countDistinctReviewingViolationsByProject(projectId)
- annotationWithoutViolation = totalAnnotations - distinctViolationReviewings
- policyComplianceRate = (annotationWithoutViolation / totalAnnotations) * 100
- weightedViolationScore = high*1.0 + medium*0.5 + low*0.2 + critical*1.5
- weightedComplianceAdjust = max(0, 100 - (weightedViolationScore / totalAnnotations * 100))
- improvementRate = improvementsFound / totalAnnotations * 100
- overallQualityScore = 0.45*annotationAccuracy + 0.35*weightedComplianceAdjust + 0.2*improvementRate
- qualityLevel: EXCELLENT>=80; GOOD>=60; FAIR>=40; POOR<40

## 5. APIs
- GET /api/analytics/projects/{projectId}/summary
- GET /api/analytics/projects/{projectId}/progress
- GET /api/analytics/projects/{projectId}/quality
- GET /api/analytics/projects/{projectId}/contributions
- GET /api/analytics/projects/{projectId}/components
- GET /api/analytics/projects/{projectId}/member-scores
- GET /api/analytics/projects/{projectId}/violations
- GET /api/analytics/projects/{projectId}/violations/{violationId}
- GET /api/analytics/projects/{projectId}/violations/summary

### 5.1 Violation API (mới)
- GET /api/analytics/projects/{projectId}/violations
  - Trả về danh sách violation dự án: violationId, assignmentId, annotatorId, reviewerId, policyId, labelId, dataItemId, reviewingId, description, createdAt, updatedAt
- GET /api/analytics/projects/{projectId}/violations/{violationId}
  - Trả chi tiết một violation
- (Optional) POST /api/analytics/projects/{projectId}/violations (khởi tạo violation từ script admin)

Security: MANAGER only (@PreAuthorize("hasRole('MANAGER')"))

## 6. Frontend call
- src/api/analyticsApi.js
- src/pages/Manager/ProjectOverview.tsx

## 7. Kiểm tra
- mvnw clean test -q pass

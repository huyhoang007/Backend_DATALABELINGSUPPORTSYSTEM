# Environment Setup Guide 🚀

## ✅ Hoàn tất - Các file đã cấu hình sẵn

### 1. `.env` file (Tự động - không commit lên Git)
- Chứa tất cả sensitive credentials
- Đã cấu hình Neon PostgreSQL + Azure Storage
- **Important**: Không đăng tải lên GitHub (đã trong `.gitignore`)

### 2. `application.yaml` (Cấu hình Spring Boot)
- Sử dụng environment variables từ `.env`
- Format: `${VARIABLE_NAME}`
- Có fallback values mặc định

### 3. `run-backend.ps1` & `start-dev.ps1`
- Tự động load `.env` trước khi chạy ứng dụng
- Không cần set environment variables thủ công

---

## 🚀 Cách chạy Backend

### Option 1: Chạy script đơn giản (Recomm ended)
```powershell
cd Backend_DATALABELINGSUPPORTSYSTEM
.\start-dev.ps1
```

### Option 2: Chạy Maven trực tiếp
```powershell
cd Backend_DATALABELINGSUPPORTSYSTEM
.\mvnw.cmd spring-boot:run
```

### Option 3: Từ IDE (IntelliJ/VS Code)
- IDE sẽ tự động load environment variables từ `.env`
- Chạy Main class: `DataLabelingSupportSystemApplication`

---

## 📋 Environment Variables

| Variable | Value | Purpose |
|----------|-------|---------|
| `DB_HOST` | ep-sparkling-voice-a1hd2z73-pooler.ap-southeast-1.aws.neon.tech | Database host |
| `DB_PORT` | 5432 | Database port |
| `DB_NAME` | datalabelingg | Database name |
| `DB_USERNAME` | neondb_owner | Database user |
| `DB_PASSWORD` | npg_rpGiWz7kZu4X | Database password |
| `AZURE_STORAGE_CONNECTION_STRING` | ... | Azure Blob Storage |
| `AZURE_STORAGE_CONTAINER` | uploads | Container name |
| `SERVER_PORT` | 8080 | Spring Boot server port |
| `JWT_SECRET` | ... | JWT secret key |
| `JWT_EXPIRATION` | 86400000 | JWT expiration (1 day) |
| `UPLOAD_PATH` | uploads | File upload directory |

---

## ⚠️ Security Reminders

1. **Never commit `.env` to Git** ✅ (Already in `.gitignore`)
2. **Never share credentials** in plain text
3. **Rotate credentials** if exposed
4. **Use `.env` only for development**
5. **Production**: Use environment variables từ deployment platform

---

## 🔧 Troubleshooting

### Database connection fails
- Check if Neon DB is running
- Verify `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`
- Test connection: `psql -h <DB_HOST> -U <DB_USERNAME>`

### Azure Storage fails
- Verify `AZURE_STORAGE_CONNECTION_STRING`
- Check container name: `AZURE_STORAGE_CONTAINER`

### Environment variables not loaded
- Restart PowerShell/IDE
- Run: `.\start-dev.ps1` (has built-in loader)

---

## 📝 Application Startup

Khi chạy, bạn sẽ thấy:
```
✅ Loaded environment variables from .env
✅ Database connected to Neon
✅ Azure Storage connected
🚀 Spring Boot started on http://localhost:8080
```

---

## ❓ Questions?

Tham khảo các file:
- `application.yaml` - Spring configuration
- `.env` - Environment variables
- `run-backend.ps1` / `start-dev.ps1` - Startup scripts

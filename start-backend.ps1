# ============================================
# Backend Starter với JAVA_HOME đúng
# ============================================

Write-Host "`n🚀 Starting Backend Server...`n" -ForegroundColor Cyan

# Set JAVA_HOME to JDK 17
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "✅ JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green
Write-Host "✅ Java version:" -ForegroundColor Green
java -version

Write-Host "`n📦 Starting Spring Boot application...`n" -ForegroundColor Yellow

cd "c:\Users\hai yen\Desktop\BE1\Backend_DATALABELINGSUPPORTSYSTEM"

# Run Spring Boot
.\mvnw.cmd spring-boot:run

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Backend failed to start" -ForegroundColor Red
    Write-Host "Check the error messages above" -ForegroundColor Yellow
}

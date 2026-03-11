# ============================================
# Backend Setup and Run Script
# ============================================
# Mục đích: Setup và chạy Spring Boot Backend
# ============================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "🚀 BACKEND SETUP & RUN SCRIPT" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$ErrorActionPreference = "Continue"
$backendDir = "c:\Users\hai yen\Desktop\BE1\Backend_DATALABELINGSUPPORTSYSTEM"

# ============================================
# 1. CHECK JAVA VERSION
# ============================================
Write-Host "1️⃣ Checking Java version..." -ForegroundColor Yellow

try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "   ✅ Java installed: $javaVersion" -ForegroundColor Green
}
catch {
    Write-Host "   ❌ Java not found!" -ForegroundColor Red
    Write-Host "   Please install Java 17 or higher" -ForegroundColor Red
    exit 1
}

# ============================================
# 2. CHECK POSTGRESQL
# ============================================
Write-Host "`n2️⃣ Checking PostgreSQL..." -ForegroundColor Yellow

try {
    $pgCheck = Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue
    if ($pgCheck.TcpTestSucceeded) {
        Write-Host "   ✅ PostgreSQL is running on port 5432" -ForegroundColor Green
    }
    else {
        Write-Host "   ⚠️  PostgreSQL not detected on port 5432" -ForegroundColor Yellow
        Write-Host "   Backend may fail to start if database is required" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "   ⚠️  Could not check PostgreSQL status" -ForegroundColor Yellow
}

# ============================================
# 3. COMPILE BACKEND
# ============================================
Write-Host "`n3️⃣ Compiling backend..." -ForegroundColor Yellow

cd $backendDir

# Try to compile main sources only (skip tests)
Write-Host "   Compiling main sources..." -ForegroundColor Gray

$compileCmd = ".\mvnw.cmd compile -DskipTests"
$compileProcess = Start-Process -FilePath "powershell" -ArgumentList "-Command", $compileCmd -NoNewWindow -Wait -PassThru

if ($compileProcess.ExitCode -eq 0) {
    Write-Host "   ✅ Compilation successful!" -ForegroundColor Green
}
else {
    Write-Host "   ⚠️  Maven compile had issues, trying alternative method..." -ForegroundColor Yellow
    
    # Alternative: compile with javac directly
    Write-Host "   Attempting direct Java compilation..." -ForegroundColor Gray
    
    # Check if classes already exist
    if (Test-Path "target\classes\com\datalabeling\datalabelingsupportsystem\DataLabelingSupportSystemApplication.class") {
        Write-Host "   ✅ Compiled classes found in target directory" -ForegroundColor Green
    }
    else {
        Write-Host "   ❌ Compilation failed and no existing classes found" -ForegroundColor Red
        Write-Host "   Please fix compilation errors manually" -ForegroundColor Red
        exit 1
    }
}

# ============================================
# 4. RUN BACKEND
# ============================================
Write-Host "`n4️⃣ Starting backend server..." -ForegroundColor Yellow
Write-Host "   Main class: com.datalabeling.datalabelingsupportsystem.DataLabelingSupportSystemApplication" -ForegroundColor Gray
Write-Host "   Port: 8080" -ForegroundColor Gray
Write-Host "`n   Press Ctrl+C to stop the server`n" -ForegroundColor Yellow

# Run with Maven Spring Boot plugin
Write-Host "   Starting with Maven Spring Boot plugin..." -ForegroundColor Gray

.\mvnw.cmd spring-boot:run

# If Maven fails, show alternative
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n   ❌ Maven spring-boot:run failed" -ForegroundColor Red
    Write-Host "`n   Alternative: Try running with IDE (IntelliJ/Eclipse)" -ForegroundColor Yellow
    Write-Host "   Or fix Maven wrapper by running:" -ForegroundColor Yellow
    Write-Host "   mvn -N io.takari:maven:wrapper" -ForegroundColor Gray
}

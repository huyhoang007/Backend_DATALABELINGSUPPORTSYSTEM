# Script khoi dong backend voi Neon DB + Azure Blob
# Chay: .\start-dev.ps1

# Tuyến chọn: tự động load từ .env file
function Load-EnvFile {
    if (Test-Path ".\.env") {
        Get-Content ".\.env" | ForEach-Object {
            if ($_ -match '^\s*([^#][^=]*?)=(.*)$') {
                $key = $matches[1].Trim()
                $value = $matches[2].Trim()
                $value = $value -replace '^["'']|["'']$', ''
                [Environment]::SetEnvironmentVariable($key, $value, "Process")
            }
        }
        Write-Host "✅ Loaded environment variables from .env" -ForegroundColor Green
    }
}

Load-EnvFile

# Hoặc set thủ công (nếu không có .env)
# $env:DB_HOST = "ep-sparkling-voice-a1hd2z73-pooler.ap-southeast-1.aws.neon.tech"
# $env:DB_PORT = "5432"
# $env:DB_NAME = "datalabelingg"
# $env:DB_USERNAME = "neondb_owner"
# $env:DB_PASSWORD = "npg_rpGiWz7kZu4X"
# $env:AZURE_STORAGE_CONNECTION_STRING = "..."
# $env:AZURE_STORAGE_CONTAINER = "uploads"

Write-Host ">> Ket noi Neon DB" -ForegroundColor Cyan
Write-Host ">> Ket noi Azure Blob: datalabelstore/uploads" -ForegroundColor Cyan
Write-Host ">> Khoi dong Spring Boot..." -ForegroundColor Green

.\mvnw.cmd spring-boot:run

# Script khoi dong backend voi Render DB + Azure Blob
# Chay: .\start-dev.ps1

$env:DATABASE_URL = "jdbc:postgresql://dpg-d6np6oshg0os73c93r50-a.singapore-postgres.render.com:5432/datalabeling"
$env:DB_USERNAME = "datalabeling_user"
$env:DB_PASSWORD = "gHxMAP0qunUnPAbwceIaGQUQPgCVr3uE"

# $env:AZURE_STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=datalabelstore;AccountKey=yxwlHqiy7ee8vqoNsty5SKdg/"
# $env:AZURE_STORAGE_CONTAINER = "uploads"

Write-Host ">> Ket noi Render DB: datalabeling" -ForegroundColor Cyan
# Write-Host ">> Ket noi Azure Blob: datalabelstore/uploads" -ForegroundColor Cyan
Write-Host ">> Using local storage" -ForegroundColor Cyan
Write-Host ">> Khoi dong Spring Boot..." -ForegroundColor Green

.\mvnw.cmd spring-boot:run

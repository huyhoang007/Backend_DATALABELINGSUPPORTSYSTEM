package com.datalabeling.datalabelingsupportsystem.service.Azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class AzureBlobService {

    private final BlobContainerClient containerClient;
    private final String localUploadPath;
    private final boolean azureEnabled;

    public AzureBlobService(
            @Value("${azure.storage.connection-string:}") String connectionString,
            @Value("${azure.storage.container-name:uploads}") String containerName,
            @Value("${app.upload.path:uploads}") String uploadPath) {

        this.localUploadPath = uploadPath;

        if (connectionString != null && !connectionString.isBlank()) {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
            }
            azureEnabled = true;
            log.info("[Azure Blob] Đã kết nối với container: {}", containerName);
        } else {
            containerClient = null;
            azureEnabled = false;
            log.info("[Azure Blob] Không có chuỗi kết nối — sử dụng đĩa cứng cục bộ: {}", uploadPath);
        }
    }

    /**
     * Upload file. blobName ví dụ: "project_15/uuid.jpg"
     * - Nếu có Azure config → upload lên Blob Storage
     * - Nếu không → lưu local disk (dùng cho dev)
     */
    public void uploadFile(String blobName, byte[] data, String contentType) throws IOException {
        if (azureEnabled) {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(new ByteArrayInputStream(data), data.length, true);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
        } else {
            Path path = Paths.get(localUploadPath).resolve(blobName).normalize();
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        }
    }

    /**
     * Download file. Trả về null nếu không tìm thấy.
     */
    public byte[] downloadFile(String blobName) throws IOException {
        if (azureEnabled) {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            return blobClient.exists() ? blobClient.downloadContent().toBytes() : null;
        } else {
            Path path = Paths.get(localUploadPath).resolve(blobName).normalize();
            return Files.exists(path) ? Files.readAllBytes(path) : null;
        }
    }
}

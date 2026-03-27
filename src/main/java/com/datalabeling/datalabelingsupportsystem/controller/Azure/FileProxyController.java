package com.datalabeling.datalabelingsupportsystem.controller.Azure;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datalabeling.datalabelingsupportsystem.service.Azure.AzureBlobService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class FileProxyController {

    private final AzureBlobService azureBlobService;

    /**
     * Serve ảnh từ Azure Blob Storage (hoặc local disk khi dev).
     * Frontend gọi /uploads/project_X/file.jpg → controller này xử lý.
     */
    @GetMapping("/uploads/**")
    public ResponseEntity<byte[]> serveFile(HttpServletRequest request) throws IOException {
        String fullPath = request.getRequestURI();
        if (!fullPath.startsWith("/uploads/")) {
            return ResponseEntity.badRequest().build();
        }
        String blobName = fullPath.substring("/uploads/".length());
        if (blobName.isBlank() || blobName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        byte[] bytes = azureBlobService.downloadFile(blobName);
        if (bytes == null) return ResponseEntity.notFound().build();

        String contentType = detectContentType(blobName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    private String detectContentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}

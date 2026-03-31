package com.snrt.knowledgebase.domain.document.service;

import com.snrt.knowledgebase.common.constants.Constants;
import com.snrt.knowledgebase.domain.document.entity.Document;
import com.snrt.knowledgebase.common.exception.DocumentException;
import com.snrt.knowledgebase.infrastructure.storage.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final MinioService minioService;

    public StorageResult storeFile(String knowledgeBaseId, String originalFilename,
                                    InputStream inputStream, long fileSize, String contentType) {
        try {
            String extension = getFileExtension(originalFilename);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String objectName = String.format("%s%s/%s-%s.%s",
                    Constants.File.MINIO_DOCUMENT_PREFIX,
                    timestamp, UUID.randomUUID().toString(), knowledgeBaseId, extension);

            minioService.uploadFile(inputStream, objectName, fileSize, contentType);

            Path uploadPath = Paths.get(Constants.File.UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String localFilename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadPath.resolve(localFilename);

            return new StorageResult(filePath.toString(), objectName, extension);
        } catch (Exception e) {
            log.error("文件存储失败: {}", e.getMessage(), e);
            throw DocumentException.uploadFailed(e.getMessage(), e);
        }
    }

    public InputStream retrieveFile(String objectName, String filePath) {
        if (objectName != null && minioService.fileExists(objectName)) {
            try {
                log.info("从MinIO下载文档: objectName={}", objectName);
                return minioService.downloadFile(objectName);
            } catch (Exception e) {
                log.warn("从MinIO下载失败，尝试本地文件: objectName={}, error={}", objectName, e.getMessage());
            }
        }

        try {
            Path localPath = Paths.get(filePath);
            if (Files.exists(localPath)) {
                log.info("从本地下载文档: path={}", filePath);
                return Files.newInputStream(localPath);
            }
        } catch (Exception e) {
            log.error("本地文件读取失败: path={}, error={}", filePath, e.getMessage());
        }

        throw DocumentException.fileReadError("文件不存在或无法读取");
    }

    public void deleteFile(String objectName, String filePath) {
        if (objectName != null) {
            try {
                minioService.deleteFile(objectName);
                log.info("MinIO文件删除成功: objectName={}", objectName);
            } catch (Exception e) {
                log.warn("MinIO文件删除失败: objectName={}, error={}", objectName, e.getMessage());
            }
        }

        try {
            Path localPath = Paths.get(filePath);
            if (Files.exists(localPath)) {
                Files.delete(localPath);
                log.info("本地文件删除成功: path={}", filePath);
            }
        } catch (Exception e) {
            log.warn("本地文件删除失败: path={}, error={}", filePath, e.getMessage());
        }
    }

    public String getPresignedUrl(String objectName, int expiryHours) {
        try {
            return minioService.getPresignedUrl(objectName, expiryHours);
        } catch (Exception e) {
            log.warn("获取MinIO预签名URL失败: objectName={}, error={}", objectName, e.getMessage());
            throw DocumentException.fileReadError("无法生成文件访问链接");
        }
    }

    public Path ensureLocalFileForProcessing(Document doc) {
        try {
            Path localPath = Paths.get(doc.getFilePath());
            if (Files.exists(localPath) && Files.size(localPath) > 0) {
                return localPath;
            }
            if (doc.getObjectName() != null && minioService.fileExists(doc.getObjectName())) {
                Path uploadPath = Paths.get(Constants.File.UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String ext = (doc.getType() != null && !doc.getType().isEmpty())
                        ? doc.getType()
                        : "bin";
                String localFilename = UUID.randomUUID().toString() + "." + ext;
                Path newPath = uploadPath.resolve(localFilename);
                try (InputStream in = minioService.downloadFile(doc.getObjectName())) {
                    Files.copy(in, newPath);
                }
                doc.setFilePath(newPath.toString());
                log.info("已从 MinIO 恢复本地文件用于向量化: documentId={}, path={}", doc.getId(), newPath);
                return newPath;
            }
        } catch (Exception e) {
            log.error("准备本地文件失败: documentId={}, error={}", doc.getId(), e.getMessage(), e);
            throw DocumentException.fileReadError("无法读取源文件，请确认文件仍存在于存储中: " + e.getMessage());
        }
        throw DocumentException.fileReadError("本地与对象存储中均未找到可用文件，无法重新向量化");
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public record StorageResult(String filePath, String objectName, String extension) {}
}

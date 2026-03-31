package com.snrt.knowledgebase.domain.document.service;

import com.snrt.knowledgebase.domain.document.dto.DocumentPreviewDTO;
import com.snrt.knowledgebase.domain.document.entity.Document;
import com.snrt.knowledgebase.common.enums.PreviewType;
import com.snrt.knowledgebase.common.exception.DocumentException;
import com.snrt.knowledgebase.domain.document.repository.DocumentRepository;
import com.snrt.knowledgebase.infrastructure.storage.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文档预览编排：按 {@link PreviewType} 决定返回纯文本、下载 URL 或错误说明。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPreviewService {

    private static final String API_DOWNLOAD_PREFIX = "/api/document/";

    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final DocumentTextExtractor documentTextExtractor;

    @Transactional(readOnly = true)
    public DocumentPreviewDTO previewDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> DocumentException.notFound(id));

        DocumentPreviewDTO preview = new DocumentPreviewDTO();
        preview.setId(doc.getId());
        preview.setName(doc.getName());
        preview.setType(doc.getType());
        preview.setSize(doc.getSize());
        preview.setKnowledgeBaseName(doc.getKnowledgeBase() != null ? doc.getKnowledgeBase().getName() : "");
        preview.setUploadTime(doc.getCreateTime());

        PreviewType previewType = PreviewType.fromExtension(doc.getType());
        preview.setPreviewType(previewType.getCode());

        if (previewType == PreviewType.UNSUPPORTED) {
            preview.setErrorMessage("该文件类型暂不支持预览，请下载后查看");
            preview.setDownloadUrl(buildDownloadPath(id));
            return preview;
        }

        if (previewType.requiresClientDownloadForRendering()) {
            preview.setDownloadUrl(buildDownloadPath(id));
            return preview;
        }

        try (InputStream inputStream = openDocumentStream(doc)) {
            if (inputStream == null) {
                preview.setErrorMessage("无法读取文件内容");
                return preview;
            }
            try {
                String content = documentTextExtractor.extractPlainText(inputStream, doc.getType());
                preview.setContent(content);
            } catch (DocumentException e) {
                log.warn("文档预览文本提取失败: id={}, error={}", id, e.getMessage());
                preview.setErrorMessage("文件内容提取失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("文档预览失败: id={}, error={}", id, e.getMessage(), e);
            preview.setErrorMessage("文件内容提取失败: " + e.getMessage());
        }

        return preview;
    }

    private static String buildDownloadPath(String documentId) {
        return API_DOWNLOAD_PREFIX + documentId + "/download";
    }

    private InputStream openDocumentStream(Document doc) {
        if (doc.getObjectName() != null && minioService.fileExists(doc.getObjectName())) {
            try {
                return minioService.downloadFile(doc.getObjectName());
            } catch (Exception e) {
                log.warn("从MinIO获取文件失败: id={}, error={}", doc.getId(), e.getMessage());
            }
        }

        try {
            Path localPath = Paths.get(doc.getFilePath());
            if (Files.exists(localPath)) {
                return Files.newInputStream(localPath);
            }
        } catch (Exception e) {
            log.error("本地文件读取失败: id={}, error={}", doc.getId(), e.getMessage());
        }

        return null;
    }
}

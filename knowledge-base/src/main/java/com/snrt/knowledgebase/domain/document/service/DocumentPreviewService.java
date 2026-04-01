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
 * 文档预览服务
 * 
 * 根据文档类型和预览策略，提供不同的预览方式：
 * - 直接文本预览：适用于支持的文本类文件
 * - 下载预览：适用于需要客户端渲染的文件
 * - 错误提示：适用于不支持的文件类型
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPreviewService {

    private static final String API_DOWNLOAD_PREFIX = "/api/document/";

    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final DocumentTextExtractor documentTextExtractor;

    /**
     * 文档预览入口方法
     * 
     * 根据文档类型和预览策略，返回不同的预览结果：
     * - 文本预览：直接返回文档内容
     * - 下载预览：返回下载URL
     * - 错误提示：返回错误信息和下载URL
     * 
     * @param id 文档ID
     * @return 文档预览DTO
     */
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

    /**
     * 构建下载路径
     * 
     * @param documentId 文档ID
     * @return 下载URL路径
     */
    private static String buildDownloadPath(String documentId) {
        return API_DOWNLOAD_PREFIX + documentId + "/download";
    }

    /**
     * 打开文档流
     * 
     * 优先从MinIO下载，如果失败则尝试从本地文件系统读取
     * 
     * @param doc 文档实体
     * @return 文档输入流
     */
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

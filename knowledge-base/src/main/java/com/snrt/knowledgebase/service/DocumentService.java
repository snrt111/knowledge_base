package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.constants.Constants;
import com.snrt.knowledgebase.dto.DocumentDTO;
import com.snrt.knowledgebase.dto.DocumentPreviewDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.enums.PreviewType;
import com.snrt.knowledgebase.exception.DocumentException;
import com.snrt.knowledgebase.exception.ResourceNotFoundException;
import com.snrt.knowledgebase.mapper.DocumentMapper;
import com.snrt.knowledgebase.repository.DocumentRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final MinioService minioService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentMapper documentMapper;

    @Transactional(readOnly = true)
    public PageResult<DocumentDTO> listDocuments(Integer page, Integer size, String knowledgeBaseId, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        Page<Document> docPage;

        if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
            if (keyword != null && !keyword.isEmpty()) {
                docPage = documentRepository.findByKnowledgeBaseIdAndNameContainingAndIsDeletedFalse(
                        knowledgeBaseId, keyword, pageable);
            } else {
                docPage = documentRepository.findByKnowledgeBaseIdAndIsDeletedFalse(knowledgeBaseId, pageable);
            }
        } else {
            docPage = documentRepository.findByIsDeletedFalse(pageable);
        }

        List<DocumentDTO> dtoList = documentMapper.toDTOList(docPage.getContent());
        return PageResult.of(dtoList, docPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public DocumentDTO getDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> DocumentException.notFound(id));
        return documentMapper.toDTO(doc);
    }

    @Transactional
    public DocumentDTO uploadDocument(String knowledgeBaseId, MultipartFile file) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库", knowledgeBaseId));

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (file.getSize() > Constants.File.MAX_SIZE) {
            throw DocumentException.fileTooLarge(String.valueOf(file.getSize()));
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = String.format("%s%s/%s-%s.%s",
                Constants.File.MINIO_DOCUMENT_PREFIX,
                timestamp, UUID.randomUUID().toString(), kb.getId(), extension);

        try {
            minioService.uploadFile(file, objectName);

            Path uploadPath = Paths.get(Constants.File.UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String localFilename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadPath.resolve(localFilename);
            Files.copy(file.getInputStream(), filePath);

            Document doc = new Document();
            doc.setName(originalFilename);
            doc.setFilePath(filePath.toString());
            doc.setObjectName(objectName);
            doc.setType(extension);
            doc.setSize(file.getSize());
            doc.setKnowledgeBase(kb);
            doc.setStatus(Document.DocumentStatus.PROCESSING);

            Document saved = documentRepository.save(doc);

            processDocumentAsync(saved);

            log.info("文档上传成功: id={}, name={}, objectName={}", saved.getId(), originalFilename, objectName);
            return documentMapper.toDTO(saved);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw DocumentException.uploadFailed(e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> DocumentException.notFound(id));
        doc.setIsDeleted(true);
        documentRepository.save(doc);

        try {
            documentProcessingService.deleteDocumentFromVectorStore(id);
            log.info("向量存储中删除成功: documentId={}", id);
        } catch (Exception e) {
            log.warn("向量存储中删除失败: documentId={}, error={}", id, e.getMessage());
        }

        if (doc.getObjectName() != null) {
            try {
                minioService.deleteFile(doc.getObjectName());
                log.info("MinIO文件删除成功: objectName={}", doc.getObjectName());
            } catch (Exception e) {
                log.warn("MinIO文件删除失败: objectName={}, error={}", doc.getObjectName(), e.getMessage());
            }
        }

        try {
            Path localPath = Paths.get(doc.getFilePath());
            if (Files.exists(localPath)) {
                Files.delete(localPath);
                log.info("本地文件删除成功: path={}", doc.getFilePath());
            }
        } catch (Exception e) {
            log.warn("本地文件删除失败: path={}, error={}", doc.getFilePath(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public InputStream downloadDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> DocumentException.notFound(id));

        if (doc.getObjectName() != null && minioService.fileExists(doc.getObjectName())) {
            try {
                log.info("从MinIO下载文档: id={}, objectName={}", id, doc.getObjectName());
                return minioService.downloadFile(doc.getObjectName());
            } catch (Exception e) {
                log.warn("从MinIO下载失败，尝试本地文件: id={}, error={}", id, e.getMessage());
            }
        }

        try {
            Path localPath = Paths.get(doc.getFilePath());
            if (Files.exists(localPath)) {
                log.info("从本地下载文档: id={}, path={}", id, doc.getFilePath());
                return Files.newInputStream(localPath);
            }
        } catch (Exception e) {
            log.error("本地文件读取失败: id={}, error={}", id, e.getMessage());
        }

        throw DocumentException.fileReadError("文件不存在或无法读取");
    }

    @Transactional(readOnly = true)
    public String getDocumentUrl(String id, int expiryHours) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> DocumentException.notFound(id));

        if (doc.getObjectName() != null) {
            try {
                return minioService.getPresignedUrl(doc.getObjectName(), expiryHours);
            } catch (Exception e) {
                log.warn("获取MinIO预签名URL失败: id={}, error={}", id, e.getMessage());
            }
        }

        return null;
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> listDocumentsByKnowledgeBase(String knowledgeBaseId) {
        return documentMapper.toDTOList(
                documentRepository.findByKnowledgeBaseIdAndIsDeletedFalse(knowledgeBaseId)
        );
    }

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
            preview.setDownloadUrl("/api/document/" + id + "/download");
            return preview;
        }

        // PDF、Word、Excel、PPT 使用前端预览库渲染，需要提供下载URL
        if (previewType == PreviewType.PDF || previewType == PreviewType.WORD
                || previewType == PreviewType.EXCEL || previewType == PreviewType.PPT) {
            preview.setDownloadUrl("/api/document/" + id + "/download");
            return preview;
        }

        // 文本类型（txt、md）提取内容返回
        try {
            InputStream inputStream = getDocumentInputStream(doc);
            if (inputStream == null) {
                preview.setErrorMessage("无法读取文件内容");
                return preview;
            }

            String content = extractTextContent(inputStream, doc.getType());
            preview.setContent(content);
        } catch (Exception e) {
            log.error("文档预览失败: id={}, error={}", id, e.getMessage(), e);
            preview.setErrorMessage("文件内容提取失败: " + e.getMessage());
        }

        return preview;
    }

    private InputStream getDocumentInputStream(Document doc) {
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

    private String extractTextContent(InputStream inputStream, String fileType) {
        try {
            DocumentReader reader = new TikaDocumentReader(new InputStreamResource(inputStream));
            List<org.springframework.ai.document.Document> documents = reader.read();

            if (documents.isEmpty()) {
                return "";
            }

            StringBuilder content = new StringBuilder();
            for (org.springframework.ai.document.Document document : documents) {
                content.append(document.getText()).append("\n");
            }

            return content.toString().trim();
        } catch (Exception e) {
            log.error("文本提取失败: error={}", e.getMessage(), e);
            throw new RuntimeException("文本提取失败: " + e.getMessage());
        }
    }


    @Async(Constants.Async.DOCUMENT_PROCESSOR_EXECUTOR)
    public CompletableFuture<Void> processDocumentAsync(Document document) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path filePath = Paths.get(document.getFilePath());
                String knowledgeBaseId = document.getKnowledgeBase().getId();

                eventPublisher.publishStatusChanged(this, document, Document.DocumentStatus.PROCESSING);

                documentProcessingService.processAndIndexDocument(filePath, document.getId(), knowledgeBaseId);

                Document.DocumentStatus oldStatus = document.getStatus();
                document.setStatus(Document.DocumentStatus.COMPLETED);
                documentRepository.save(document);

                eventPublisher.publishStatusChanged(this, document, oldStatus, Document.DocumentStatus.COMPLETED);

                log.info("文档处理完成: id={}, name={}", document.getId(), document.getName());
            } catch (Exception e) {
                Document.DocumentStatus oldStatus = document.getStatus();
                document.setStatus(Document.DocumentStatus.FAILED);
                documentRepository.save(document);

                eventPublisher.publishStatusChanged(this, document, oldStatus, Document.DocumentStatus.FAILED);

                log.error("文档处理失败: id={}, name={}, error={}",
                        document.getId(), document.getName(), e.getMessage(), e);
            }
        });
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}

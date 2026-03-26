package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.dto.DocumentDTO;
import com.snrt.knowledgebase.dto.DocumentPreviewDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.repository.DocumentRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final MinioService minioService;
    private final DocumentProcessingService documentProcessingService;

    private static final String UPLOAD_DIR = "uploads/documents/";

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

        List<DocumentDTO> dtoList = docPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, docPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public DocumentDTO getDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        return convertToDTO(doc);
    }

    @Transactional
    public DocumentDTO uploadDocument(String knowledgeBaseId, MultipartFile file) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = String.format("documents/%s/%s-%s.%s", 
                timestamp, UUID.randomUUID().toString(), kb.getId(), extension);

        try {
            // 上传到MinIO
            minioService.uploadFile(file, objectName);

            // 同时保存到本地作为备份
            Path uploadPath = Paths.get(UPLOAD_DIR);
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
            return convertToDTO(saved);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        doc.setIsDeleted(true);
        documentRepository.save(doc);

        // 从向量存储中删除文档向量
        try {
            documentProcessingService.deleteDocumentFromVectorStore(id);
            log.info("向量存储中删除成功: documentId={}", id);
        } catch (Exception e) {
            log.warn("向量存储中删除失败: documentId={}, error={}", id, e.getMessage());
        }

        // 删除MinIO中的文件
        if (doc.getObjectName() != null) {
            try {
                minioService.deleteFile(doc.getObjectName());
                log.info("MinIO文件删除成功: objectName={}", doc.getObjectName());
            } catch (Exception e) {
                log.warn("MinIO文件删除失败: objectName={}, error={}", doc.getObjectName(), e.getMessage());
            }
        }

        // 删除本地文件
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
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 优先从MinIO下载
        if (doc.getObjectName() != null && minioService.fileExists(doc.getObjectName())) {
            try {
                log.info("从MinIO下载文档: id={}, objectName={}", id, doc.getObjectName());
                return minioService.downloadFile(doc.getObjectName());
            } catch (Exception e) {
                log.warn("从MinIO下载失败，尝试本地文件: id={}, error={}", id, e.getMessage());
            }
        }

        // 从本地文件下载
        try {
            Path localPath = Paths.get(doc.getFilePath());
            if (Files.exists(localPath)) {
                log.info("从本地下载文档: id={}, path={}", id, doc.getFilePath());
                return Files.newInputStream(localPath);
            }
        } catch (Exception e) {
            log.error("本地文件读取失败: id={}, error={}", id, e.getMessage());
        }

        throw new RuntimeException("文件不存在或无法读取");
    }

    @Transactional(readOnly = true)
    public String getDocumentUrl(String id, int expiryHours) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

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
        return documentRepository.findByKnowledgeBaseIdAndIsDeletedFalse(knowledgeBaseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentPreviewDTO previewDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        DocumentPreviewDTO previewDTO = new DocumentPreviewDTO();
        previewDTO.setId(doc.getId());
        previewDTO.setName(doc.getName());
        previewDTO.setType(doc.getType());
        previewDTO.setSize(doc.getSize());
        previewDTO.setKnowledgeBaseName(doc.getKnowledgeBase().getName());
        previewDTO.setUploadTime(doc.getCreateTime());

        // 根据文件类型设置内容
        if (isTextFile(doc.getType())) {
            try {
                String content = readTextContent(doc);
                previewDTO.setContent(content);
                previewDTO.setPreviewType("text");
            } catch (Exception e) {
                log.error("读取文本文件失败: id={}, error={}", id, e.getMessage());
                previewDTO.setPreviewType("unsupported");
                previewDTO.setErrorMessage("无法读取文件内容: " + e.getMessage());
            }
        } else if (isPdfFile(doc.getType())) {
            previewDTO.setPreviewType("pdf");
            previewDTO.setDownloadUrl("/api/document/" + id + "/download");
        } else if (isWordFile(doc.getType())) {
            previewDTO.setPreviewType("word");
            previewDTO.setDownloadUrl("/api/document/" + id + "/download");
        } else {
            previewDTO.setPreviewType("unsupported");
            previewDTO.setDownloadUrl("/api/document/" + id + "/download");
        }

        return previewDTO;
    }

    private boolean isTextFile(String type) {
        return "txt".equalsIgnoreCase(type) || "md".equalsIgnoreCase(type) ||
               "json".equalsIgnoreCase(type) || "xml".equalsIgnoreCase(type) ||
               "html".equalsIgnoreCase(type) || "htm".equalsIgnoreCase(type) ||
               "js".equalsIgnoreCase(type) || "css".equalsIgnoreCase(type) ||
               "java".equalsIgnoreCase(type) || "py".equalsIgnoreCase(type) ||
               "sql".equalsIgnoreCase(type) || "yaml".equalsIgnoreCase(type) ||
               "yml".equalsIgnoreCase(type) || "properties".equalsIgnoreCase(type);
    }

    private boolean isPdfFile(String type) {
        return "pdf".equalsIgnoreCase(type);
    }

    private boolean isWordFile(String type) {
        return "doc".equalsIgnoreCase(type) || "docx".equalsIgnoreCase(type);
    }

    private String readTextContent(Document doc) throws Exception {
        // 优先从本地读取
        Path localPath = Paths.get(doc.getFilePath());
        if (Files.exists(localPath)) {
            return Files.readString(localPath);
        }

        // 从MinIO读取
        if (doc.getObjectName() != null) {
            try (InputStream is = minioService.downloadFile(doc.getObjectName())) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        throw new RuntimeException("文件不存在");
    }

    private void processDocumentAsync(Document document) {
        new Thread(() -> {
            try {
                // 处理文档：解析、向量化和存储到向量数据库
                Path filePath = Paths.get(document.getFilePath());
                String knowledgeBaseId = document.getKnowledgeBase().getId();
                documentProcessingService.processAndIndexDocument(filePath, document.getId(), knowledgeBaseId);
                
                document.setStatus(Document.DocumentStatus.COMPLETED);
                documentRepository.save(document);
                log.info("文档处理完成: id={}, name={}", document.getId(), document.getName());
            } catch (Exception e) {
                document.setStatus(Document.DocumentStatus.FAILED);
                documentRepository.save(document);
                log.error("文档处理失败: id={}, name={}, error={}", 
                        document.getId(), document.getName(), e.getMessage(), e);
            }
        }).start();
    }

    private DocumentDTO convertToDTO(Document doc) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(doc.getId());
        dto.setName(doc.getName());
        dto.setKnowledgeBaseId(doc.getKnowledgeBase().getId());
        dto.setKnowledgeBaseName(doc.getKnowledgeBase().getName());
        dto.setType(doc.getType());
        dto.setSize(doc.getSize());
        dto.setStatus(doc.getStatus().name().toLowerCase());
        dto.setUploadTime(doc.getCreateTime());
        return dto;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}

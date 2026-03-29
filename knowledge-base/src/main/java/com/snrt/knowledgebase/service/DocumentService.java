package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.constants.Constants;
import com.snrt.knowledgebase.dto.DocumentDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.exception.DocumentException;
import com.snrt.knowledgebase.exception.ResourceNotFoundException;
import com.snrt.knowledgebase.mapper.DocumentMapper;
import com.snrt.knowledgebase.repository.DocumentRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import com.snrt.knowledgebase.service.document.DocumentProcessor;
import com.snrt.knowledgebase.service.document.DocumentProcessorFactory;
import com.snrt.knowledgebase.service.document.DocumentStorageService;
import com.snrt.knowledgebase.service.document.DocumentStorageService.StorageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentMapper documentMapper;
    private final DocumentProcessorFactory processorFactory;
    private final DocumentStorageService storageService;

    @Qualifier("documentProcessorExecutor")
    private final Executor documentProcessorExecutor;

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

        DocumentProcessor processor = processorFactory.getProcessor(extension);
        processor.validateFile(file);

        try {
            StorageResult storageResult = storageService.storeFile(
                    knowledgeBaseId, originalFilename, file.getInputStream(), file.getSize(), file.getContentType());

            Document doc = new Document();
            doc.setName(originalFilename);
            doc.setFilePath(storageResult.filePath());
            doc.setObjectName(storageResult.objectName());
            doc.setType(storageResult.extension());
            doc.setSize(file.getSize());
            doc.setKnowledgeBase(kb);
            doc.setStatus(Document.DocumentStatus.PROCESSING);

            Document saved = documentRepository.save(doc);

            processDocumentAsync(saved.getId());

            log.info("文档上传成功: id={}, name={}, objectName={}", saved.getId(), originalFilename, storageResult.objectName());
            return documentMapper.toDTO(saved);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw DocumentException.uploadFailed(e.getMessage(), e);
        }
    }

    @Transactional
    public DocumentDTO reprocessDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalseWithKnowledgeBase(id)
                .orElseThrow(() -> DocumentException.notFound(id));

        if (doc.getStatus() == Document.DocumentStatus.PROCESSING) {
            throw DocumentException.reprocessNotAllowed("文档正在处理中，请稍后再试");
        }

        Path localPath = storageService.ensureLocalFileForProcessing(doc);

        documentProcessingService.deleteDocumentFromVectorStore(id);

        Document.DocumentStatus oldStatus = doc.getStatus();
        doc.setStatus(Document.DocumentStatus.PROCESSING);
        documentRepository.save(doc);

        eventPublisher.publishStatusChanged(this, doc, oldStatus, Document.DocumentStatus.PROCESSING);

        processDocumentAsync(doc.getId());

        log.info("已提交文档重新向量化: id={}, name={}, previousStatus={}", id, doc.getName(), oldStatus);
        return documentMapper.toDTO(doc);
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

        storageService.deleteFile(doc.getObjectName(), doc.getFilePath());
    }

    @Transactional(readOnly = true)
    public InputStream downloadDocument(String id) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> DocumentException.notFound(id));
        return storageService.retrieveFile(doc.getObjectName(), doc.getFilePath());
    }

    @Transactional(readOnly = true)
    public String getDocumentUrl(String id, int expiryHours) {
        Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> DocumentException.notFound(id));
        return storageService.getPresignedUrl(doc.getObjectName(), expiryHours);
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> listDocumentsByKnowledgeBase(String knowledgeBaseId) {
        return documentMapper.toDTOList(
                documentRepository.findByKnowledgeBaseIdAndIsDeletedFalse(knowledgeBaseId)
        );
    }

    private void processDocumentAsync(String documentId) {
        documentProcessorExecutor.execute(() -> {
            try {
                Document document = documentRepository.findByIdAndIsDeletedFalseWithKnowledgeBase(documentId)
                        .orElse(null);
                
                if (document == null) {
                    log.warn("文档不存在或已被删除，跳过处理: documentId={}", documentId);
                    return;
                }

                Path filePath = storageService.ensureLocalFileForProcessing(document);
                String knowledgeBaseId = document.getKnowledgeBase().getId();
                String knowledgeBaseName = document.getKnowledgeBase().getName();

                DocumentProcessor processor = processorFactory.getProcessor(document.getType());
                processor.processDocument(filePath, document.getId(), document.getName(),
                        knowledgeBaseId, knowledgeBaseName);

                Document.DocumentStatus oldStatus = document.getStatus();
                document.setStatus(Document.DocumentStatus.COMPLETED);
                documentRepository.save(document);

                eventPublisher.publishStatusChanged(this, document, oldStatus, Document.DocumentStatus.COMPLETED);

                log.info("文档处理完成: id={}, name={}", document.getId(), document.getName());
            } catch (Exception e) {
                try {
                    Document doc = documentRepository.findByIdAndIsDeletedFalseWithKnowledgeBase(documentId)
                            .orElse(null);
                    if (doc != null) {
                        Document.DocumentStatus oldStatus = doc.getStatus();
                        doc.setStatus(Document.DocumentStatus.FAILED);
                        documentRepository.save(doc);
                        eventPublisher.publishStatusChanged(this, doc, oldStatus, Document.DocumentStatus.FAILED);
                    }
                } catch (Exception ex) {
                    log.error("更新文档失败状态时出错: documentId={}", documentId, ex);
                }

                log.error("文档处理失败: id={}, error={}", documentId, e.getMessage(), e);
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

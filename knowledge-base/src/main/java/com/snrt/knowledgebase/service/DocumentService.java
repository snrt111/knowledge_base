package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.constants.Constants;
import com.snrt.knowledgebase.dto.BatchOperationResult;
import com.snrt.knowledgebase.dto.DocumentDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.exception.DocumentException;
import com.snrt.knowledgebase.exception.ResourceNotFoundException;
import com.snrt.knowledgebase.mapper.DocumentMapper;
import com.snrt.knowledgebase.messaging.DocumentProcessProducer;
import com.snrt.knowledgebase.repository.DocumentRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import com.snrt.knowledgebase.service.document.DocumentProcessor;
import com.snrt.knowledgebase.service.document.DocumentProcessorFactory;
import com.snrt.knowledgebase.service.document.DocumentStorageService;
import com.snrt.knowledgebase.service.document.DocumentStorageService.StorageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    private final DocumentProcessProducer documentProcessProducer;

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

            // 在事务提交后发送消息，确保消费者能查询到文档
            String finalOriginalFilename = originalFilename;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    documentProcessProducer.sendDocumentProcessMessage(
                            saved.getId(),
                            saved.getName(),
                            kb.getId(),
                            kb.getName(),
                            saved.getFilePath(),
                            saved.getObjectName(),
                            saved.getType()
                    );
                    log.info("文档上传成功: id={}, name={}, objectName={}", saved.getId(), finalOriginalFilename, storageResult.objectName());
                }
            });

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

        // 在事务提交后发送消息，确保消费者能查询到文档
        String finalId = id;
        String finalOldStatus = oldStatus.toString();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                documentProcessProducer.sendDocumentProcessMessage(
                        doc.getId(),
                        doc.getName(),
                        doc.getKnowledgeBase().getId(),
                        doc.getKnowledgeBase().getName(),
                        doc.getFilePath(),
                        doc.getObjectName(),
                        doc.getType()
                );
                log.info("已提交文档重新向量化: id={}, name={}, previousStatus={}", finalId, doc.getName(), finalOldStatus);
            }
        });

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

    @Transactional
    public BatchOperationResult<DocumentDTO> batchDeleteDocuments(List<String> documentIds) {
        BatchOperationResult<DocumentDTO> result = new BatchOperationResult<>();
        result.setTotal(documentIds.size());

        List<DocumentDTO> successItems = new ArrayList<>();
        List<BatchOperationResult.FailedItem> failedItems = new ArrayList<>();

        for (String id : documentIds) {
            try {
                Document doc = documentRepository.findByIdAndIsDeletedFalse(id)
                        .orElse(null);
                if (doc == null) {
                    failedItems.add(BatchOperationResult.FailedItem.builder()
                            .id(id)
                            .reason("文档不存在或已被删除")
                            .build());
                    continue;
                }

                doc.setIsDeleted(true);
                documentRepository.save(doc);

                try {
                    documentProcessingService.deleteDocumentFromVectorStore(id);
                } catch (Exception e) {
                    log.warn("向量存储中删除失败: documentId={}, error={}", id, e.getMessage());
                }

                storageService.deleteFile(doc.getObjectName(), doc.getFilePath());

                successItems.add(documentMapper.toDTO(doc));
                log.info("批量删除文档成功: id={}", id);
            } catch (Exception e) {
                failedItems.add(BatchOperationResult.FailedItem.builder()
                        .id(id)
                        .reason(e.getMessage())
                        .build());
                log.error("批量删除文档失败: id={}, error={}", id, e.getMessage(), e);
            }
        }

        result.setSuccess(successItems.size());
        result.setFailed(failedItems.size());
        result.setSuccessItems(successItems);
        result.setFailedItems(failedItems);

        log.info("批量删除文档完成: total={}, success={}, failed={}",
                result.getTotal(), result.getSuccess(), result.getFailed());
        return result;
    }

    @Transactional
    public BatchOperationResult<DocumentDTO> batchReprocessDocuments(List<String> documentIds) {
        BatchOperationResult<DocumentDTO> result = new BatchOperationResult<>();
        result.setTotal(documentIds.size());

        List<DocumentDTO> successItems = new ArrayList<>();
        List<BatchOperationResult.FailedItem> failedItems = new ArrayList<>();

        for (String id : documentIds) {
            try {
                Document doc = documentRepository.findByIdAndIsDeletedFalseWithKnowledgeBase(id)
                        .orElse(null);
                if (doc == null) {
                    failedItems.add(BatchOperationResult.FailedItem.builder()
                            .id(id)
                            .reason("文档不存在或已被删除")
                            .build());
                    continue;
                }

                if (doc.getStatus() == Document.DocumentStatus.PROCESSING) {
                    failedItems.add(BatchOperationResult.FailedItem.builder()
                            .id(id)
                            .reason("文档正在处理中")
                            .build());
                    continue;
                }

                storageService.ensureLocalFileForProcessing(doc);
                documentProcessingService.deleteDocumentFromVectorStore(id);

                Document.DocumentStatus oldStatus = doc.getStatus();
                doc.setStatus(Document.DocumentStatus.PROCESSING);
                documentRepository.save(doc);

                eventPublisher.publishStatusChanged(this, doc, oldStatus, Document.DocumentStatus.PROCESSING);

                // 在事务提交后发送消息，确保消费者能查询到文档
                Document finalDoc = doc;
                String finalId = id;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        documentProcessProducer.sendDocumentProcessMessage(
                                finalDoc.getId(),
                                finalDoc.getName(),
                                finalDoc.getKnowledgeBase().getId(),
                                finalDoc.getKnowledgeBase().getName(),
                                finalDoc.getFilePath(),
                                finalDoc.getObjectName(),
                                finalDoc.getType()
                        );
                        log.info("批量重新处理文档成功: id={}", finalId);
                    }
                });

                successItems.add(documentMapper.toDTO(doc));
            } catch (Exception e) {
                failedItems.add(BatchOperationResult.FailedItem.builder()
                        .id(id)
                        .reason(e.getMessage())
                        .build());
                log.error("批量重新处理文档失败: id={}, error={}", id, e.getMessage(), e);
            }
        }

        result.setSuccess(successItems.size());
        result.setFailed(failedItems.size());
        result.setSuccessItems(successItems);
        result.setFailedItems(failedItems);

        log.info("批量重新处理文档完成: total={}, success={}, failed={}",
                result.getTotal(), result.getSuccess(), result.getFailed());
        return result;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}

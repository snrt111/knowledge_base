package com.snrt.knowledgebase.messaging;

import com.snrt.knowledgebase.config.RabbitMQConfig;
import com.snrt.knowledgebase.dto.DocumentProcessMessage;
import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.repository.DocumentRepository;
import com.snrt.knowledgebase.service.DocumentEventPublisher;
import com.snrt.knowledgebase.service.DocumentProcessingService;
import com.snrt.knowledgebase.service.document.DocumentProcessor;
import com.snrt.knowledgebase.service.document.DocumentProcessorFactory;
import com.snrt.knowledgebase.service.document.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessConsumer {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentProcessorFactory processorFactory;
    private final DocumentStorageService storageService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_PROCESS_QUEUE, concurrency = "3-10")
    @Transactional
    public void handleDocumentProcess(DocumentProcessMessage message) {
        String documentId = message.getDocumentId();
        log.info("收到文档处理消息: documentId={}, documentName={}, retryCount={}",
                documentId, message.getDocumentName(), message.getRetryCount());

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
            log.error("文档处理失败: id={}, error={}", documentId, e.getMessage(), e);
            handleProcessFailure(message, e);
        }
    }

    private void handleProcessFailure(DocumentProcessMessage message, Exception e) {
        String documentId = message.getDocumentId();

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

        int maxRetryAttempts = 3;
        if (message.getRetryCount() < maxRetryAttempts) {
            message.incrementRetryCount();
            log.warn("文档处理失败，准备重试: documentId={}, retryCount={}", documentId, message.getRetryCount());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_PROCESS_EXCHANGE,
                    RabbitMQConfig.DOCUMENT_PROCESS_ROUTING_KEY,
                    message
            );
        } else {
            log.error("文档处理失败，已达最大重试次数: documentId={}, maxRetryAttempts={}",
                    documentId, maxRetryAttempts);
        }
    }
}

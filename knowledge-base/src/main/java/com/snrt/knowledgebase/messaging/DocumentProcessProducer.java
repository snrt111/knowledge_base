package com.snrt.knowledgebase.messaging;

import com.snrt.knowledgebase.config.RabbitMQConfig;
import com.snrt.knowledgebase.dto.DocumentProcessMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendDocumentProcessMessage(String documentId, String documentName,
                                           String knowledgeBaseId, String knowledgeBaseName,
                                           String filePath, String objectName, String fileType) {
        DocumentProcessMessage message = DocumentProcessMessage.builder()
                .documentId(documentId)
                .documentName(documentName)
                .knowledgeBaseId(knowledgeBaseId)
                .knowledgeBaseName(knowledgeBaseName)
                .filePath(filePath)
                .objectName(objectName)
                .fileType(fileType)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_PROCESS_EXCHANGE,
                RabbitMQConfig.DOCUMENT_PROCESS_ROUTING_KEY,
                message
        );

        log.info("文档处理消息已发送到队列: documentId={}, documentName={}", documentId, documentName);
    }
}

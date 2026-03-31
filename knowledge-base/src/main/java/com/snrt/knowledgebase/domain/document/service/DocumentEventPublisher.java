package com.snrt.knowledgebase.domain.document.service;

import com.snrt.knowledgebase.domain.document.entity.Document;
import com.snrt.knowledgebase.infrastructure.messaging.DocumentStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishStatusChanged(Object source, Document document,
                                     Document.DocumentStatus oldStatus,
                                     Document.DocumentStatus newStatus) {
        DocumentStatusChangedEvent event = new DocumentStatusChangedEvent(
                source,
                document.getId(),
                document.getName(),
                oldStatus,
                newStatus,
                document.getKnowledgeBase().getId()
        );
        eventPublisher.publishEvent(event);
        log.debug("发布文档状态变更事件: documentId={}, {} -> {}",
                document.getId(), oldStatus, newStatus);
    }

    public void publishStatusChanged(Object source, Document document,
                                     Document.DocumentStatus newStatus) {
        publishStatusChanged(source, document, document.getStatus(), newStatus);
    }
}

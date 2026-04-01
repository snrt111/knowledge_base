package com.snrt.knowledgebase.domain.document.service;

import com.snrt.knowledgebase.domain.document.entity.Document;
import com.snrt.knowledgebase.infrastructure.messaging.DocumentStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 文档事件发布器
 * 
 * 负责发布文档状态变更事件，通知其他组件进行相应处理
 * 使用Spring ApplicationEvent机制实现事件驱动架构
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布文档状态变更事件
     * 
     * @param source 事件源
     * @param document 文档实体
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
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

    /**
     * 发布文档状态变更事件（自动获取旧状态）
     * 
     * @param source 事件源
     * @param document 文档实体
     * @param newStatus 新状态
     */
    public void publishStatusChanged(Object source, Document document,
                                     Document.DocumentStatus newStatus) {
        publishStatusChanged(source, document, document.getStatus(), newStatus);
    }
}

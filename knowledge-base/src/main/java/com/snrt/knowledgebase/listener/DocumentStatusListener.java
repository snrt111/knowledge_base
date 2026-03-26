package com.snrt.knowledgebase.listener;

import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.event.DocumentStatusChangedEvent;
import com.snrt.knowledgebase.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DocumentStatusListener {

    @EventListener
    @Async
    public void onDocumentStatusChanged(DocumentStatusChangedEvent event) {
        log.info("文档状态变更: documentId={}, name={}, {} -> {}",
                event.getDocumentId(),
                event.getDocumentName(),
                event.getOldStatus(),
                event.getNewStatus());

        Map<String, Object> context = new HashMap<>();
        context.put("documentId", event.getDocumentId());
        context.put("documentName", event.getDocumentName());
        context.put("oldStatus", event.getOldStatus());
        context.put("newStatus", event.getNewStatus());
        context.put("knowledgeBaseId", event.getKnowledgeBaseId());

        LogUtils.logBusinessEvent("DOCUMENT_STATUS_CHANGED", context);

        if (event.isCompleted()) {
            handleDocumentCompleted(event);
        } else if (event.isFailed()) {
            handleDocumentFailed(event);
        }
    }

    private void handleDocumentCompleted(DocumentStatusChangedEvent event) {
        log.info("文档处理完成: documentId={}, knowledgeBaseId={}",
                event.getDocumentId(), event.getKnowledgeBaseId());

        Map<String, Object> context = new HashMap<>();
        context.put("documentId", event.getDocumentId());
        context.put("knowledgeBaseId", event.getKnowledgeBaseId());
        context.put("status", "COMPLETED");

        LogUtils.logBusinessEvent("DOCUMENT_PROCESS_COMPLETED", context);
    }

    private void handleDocumentFailed(DocumentStatusChangedEvent event) {
        log.warn("文档处理失败: documentId={}, knowledgeBaseId={}",
                event.getDocumentId(), event.getKnowledgeBaseId());

        Map<String, Object> context = new HashMap<>();
        context.put("documentId", event.getDocumentId());
        context.put("knowledgeBaseId", event.getKnowledgeBaseId());
        context.put("status", "FAILED");

        LogUtils.logBusinessEvent("DOCUMENT_PROCESS_FAILED", context);
    }
}

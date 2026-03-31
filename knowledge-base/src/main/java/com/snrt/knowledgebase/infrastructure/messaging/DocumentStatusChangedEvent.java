package com.snrt.knowledgebase.infrastructure.messaging;

import com.snrt.knowledgebase.domain.document.entity.Document;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DocumentStatusChangedEvent extends ApplicationEvent {

    private final String documentId;
    private final String documentName;
    private final Document.DocumentStatus oldStatus;
    private final Document.DocumentStatus newStatus;
    private final String knowledgeBaseId;

    public DocumentStatusChangedEvent(Object source, String documentId, String documentName,
                                      Document.DocumentStatus oldStatus, Document.DocumentStatus newStatus,
                                      String knowledgeBaseId) {
        super(source);
        this.documentId = documentId;
        this.documentName = documentName;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public boolean isCompleted() {
        return newStatus == Document.DocumentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return newStatus == Document.DocumentStatus.FAILED;
    }

    public boolean isProcessing() {
        return newStatus == Document.DocumentStatus.PROCESSING;
    }
}

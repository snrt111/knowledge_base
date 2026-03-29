package com.snrt.knowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String documentId;

    private String documentName;

    private String knowledgeBaseId;

    private String knowledgeBaseName;

    private String filePath;

    private String objectName;

    private String fileType;

    private LocalDateTime createTime;

    private int retryCount;

    public void incrementRetryCount() {
        this.retryCount++;
    }
}

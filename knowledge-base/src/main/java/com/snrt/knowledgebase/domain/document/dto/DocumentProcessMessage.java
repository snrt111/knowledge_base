package com.snrt.knowledgebase.domain.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档处理消息DTO
 * 
 * 用于RabbitMQ消息队列传递文档处理任务
 * 包含文档信息、文件路径、重试次数等
 * 
 * @author SNRT
 * @since 1.0
 */
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

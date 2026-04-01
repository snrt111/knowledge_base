package com.snrt.knowledgebase.domain.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话DTO
 * 
 * 包含会话的基本信息：标题、知识库、消息数量等
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class ChatSessionDTO {

    private String id;
    private String title;
    private String knowledgeBaseId;
    private String knowledgeBaseName;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

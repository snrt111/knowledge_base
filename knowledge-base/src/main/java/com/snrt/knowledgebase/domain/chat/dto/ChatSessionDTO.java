package com.snrt.knowledgebase.domain.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

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

package com.snrt.knowledgebase.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageDTO {

    private String id;
    private String role;
    private String content;
    private LocalDateTime createTime;

    /**
     * AI回答引用的文档来源列表
     * 仅当role为ASSISTANT时可能有值
     */
    private List<DocumentSourceDTO> documentSources;
}

package com.snrt.knowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天响应DTO
 * 包含AI回答内容和文档来源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {

    /**
     * AI回答内容
     */
    private String content;

    /**
     * 引用的文档来源列表
     */
    private List<DocumentSourceDTO> sources;
}

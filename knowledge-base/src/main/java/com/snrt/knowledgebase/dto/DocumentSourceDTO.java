package com.snrt.knowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档来源信息DTO
 * 用于记录AI回答引用的知识库文档来源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSourceDTO {

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 知识库名称
     */
    private String knowledgeBaseName;

    /**
     * 相关度分数（可选）
     */
    private Double score;

    /**
     * 引用片段内容（可选）
     */
    private String snippet;

    /**
     * 匹配的多个分块内容列表
     */
    private List<String> snippets;
}

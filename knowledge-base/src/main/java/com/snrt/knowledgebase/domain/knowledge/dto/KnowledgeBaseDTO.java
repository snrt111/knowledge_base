package com.snrt.knowledgebase.domain.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库DTO
 * 
 * 包含知识库的基本信息：名称、描述、文档数量、创建/更新时间等
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class KnowledgeBaseDTO {

    private String id;
    private String name;
    private String description;
    private Long documentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱节点DTO
 * 
 * 包含节点的基本信息：标签、名称、属性等
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class KnowledgeGraphNodeDTO {

    private String id;

    private String label;

    private String name;

    private String properties;

    private String knowledgeGraphId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

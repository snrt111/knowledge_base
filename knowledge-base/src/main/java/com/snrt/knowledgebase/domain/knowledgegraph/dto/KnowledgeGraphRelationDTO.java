package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱关系DTO
 * 
 * 包含关系的基本信息：类型、起始节点、目标节点、属性等
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class KnowledgeGraphRelationDTO {

    private String id;

    private String type;

    private String fromNodeId;

    private String toNodeId;

    private String properties;

    private String knowledgeGraphId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

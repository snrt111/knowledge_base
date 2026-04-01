package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识图谱DTO
 * 
 * 包含知识图谱的基本信息和关联的节点、关系列表
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class KnowledgeGraphDTO {

    private String id;

    private String name;

    private String description;

    private String knowledgeBaseId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer nodeCount;

    private Integer relationCount;

    private List<KnowledgeGraphNodeDTO> nodes;

    private List<KnowledgeGraphRelationDTO> relations;
}

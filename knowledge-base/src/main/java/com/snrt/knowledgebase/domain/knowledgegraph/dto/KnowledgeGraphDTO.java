package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

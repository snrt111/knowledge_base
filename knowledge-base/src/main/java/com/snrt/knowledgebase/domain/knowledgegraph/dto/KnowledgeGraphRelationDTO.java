package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import lombok.Data;

import java.time.LocalDateTime;

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

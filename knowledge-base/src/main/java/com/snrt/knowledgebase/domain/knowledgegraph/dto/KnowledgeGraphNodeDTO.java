package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import lombok.Data;

import java.time.LocalDateTime;

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

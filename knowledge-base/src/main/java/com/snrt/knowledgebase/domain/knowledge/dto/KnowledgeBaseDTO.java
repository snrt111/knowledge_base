package com.snrt.knowledgebase.domain.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseDTO {

    private String id;
    private String name;
    private String description;
    private Long documentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

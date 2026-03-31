package com.snrt.knowledgebase.domain.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentDTO {

    private String id;
    private String name;
    private String knowledgeBaseId;
    private String knowledgeBaseName;
    private String type;
    private Long size;
    private String status;
    private LocalDateTime uploadTime;
}

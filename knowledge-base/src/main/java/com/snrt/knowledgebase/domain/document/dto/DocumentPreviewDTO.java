package com.snrt.knowledgebase.domain.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentPreviewDTO {

    private String id;

    private String name;

    private String type;

    private Long size;

    private String knowledgeBaseName;

    private LocalDateTime uploadTime;

    private String previewType;

    private String content;

    private String downloadUrl;

    private String errorMessage;
}

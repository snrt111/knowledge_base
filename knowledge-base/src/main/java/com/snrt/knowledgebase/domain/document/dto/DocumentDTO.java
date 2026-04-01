package com.snrt.knowledgebase.domain.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档DTO
 * 
 * 包含文档的基本信息：名称、类型、大小、状态、上传时间等
 * 
 * @author SNRT
 * @since 1.0
 */
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

package com.snrt.knowledgebase.domain.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档预览DTO
 * 
 * 包含文档预览所需的所有信息：
 * - 文档基本信息（ID、名称、类型、大小）
 * - 预览信息（预览类型、内容、错误信息）
 * - 下载信息（下载URL）
 * 
 * @author SNRT
 * @since 1.0
 */
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

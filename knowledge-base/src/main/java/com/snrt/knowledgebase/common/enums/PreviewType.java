package com.snrt.knowledgebase.common.enums;

import lombok.Getter;

import java.util.Set;

/**
 * 预览类型枚举
 * 
 * 定义不同文档类型的预览支持情况
 * 用于判断文档是否可以预览以及预览方式
 * 
 * @author SNRT
 * @since 1.0
 */
@Getter
public enum PreviewType {

    /**
     * 文本文件
     */
    TEXT("text", "文本", Set.of("txt", "md")),

    /**
     * PDF文档
     */
    PDF("pdf", "PDF文档", Set.of("pdf")),

    /**
     * Word文档
     */
    WORD("word", "Word文档", Set.of("doc", "docx")),

    /**
     * Excel文档
     */
    EXCEL("excel", "Excel文档", Set.of("xls", "xlsx")),

    /**
     * PPT文档
     */
    PPT("ppt", "PPT文档", Set.of("ppt", "pptx")),

    /**
     * 不支持预览
     */
    UNSUPPORTED("unsupported", "不支持预览", Set.of());

    private final String code;
    private final String description;
    private final Set<String> supportedExtensions;

    PreviewType(String code, String description, Set<String> supportedExtensions) {
        this.code = code;
        this.description = description;
        this.supportedExtensions = supportedExtensions;
    }

    /**
     * 根据文件扩展名获取预览类型
     * 
     * @param extension 文件扩展名
     * @return 预览类型
     */
    public static PreviewType fromExtension(String extension) {
        if (extension == null) {
            return UNSUPPORTED;
        }
        String ext = extension.toLowerCase();
        for (PreviewType type : values()) {
            if (type.supportedExtensions.contains(ext)) {
                return type;
            }
        }
        return UNSUPPORTED;
    }

    /**
     * 判断是否可预览
     * 
     * @return 是否可预览
     */
    public boolean isPreviewable() {
        return this != UNSUPPORTED;
    }

    /**
     * 判断是否需要客户端下载渲染
     * 
     * PDF、Word、Excel、PPT需要前端通过下载URL拉取二进制后渲染
     * 
     * @return 是否需要客户端下载
     */
    public boolean requiresClientDownloadForRendering() {
        return this == PDF || this == WORD || this == EXCEL || this == PPT;
    }
}

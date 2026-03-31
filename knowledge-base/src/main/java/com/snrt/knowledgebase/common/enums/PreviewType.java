package com.snrt.knowledgebase.common.enums;

import lombok.Getter;

import java.util.Set;

@Getter
public enum PreviewType {

    TEXT("text", "文本", Set.of("txt", "md")),

    PDF("pdf", "PDF文档", Set.of("pdf")),

    WORD("word", "Word文档", Set.of("doc", "docx")),

    EXCEL("excel", "Excel文档", Set.of("xls", "xlsx")),

    PPT("ppt", "PPT文档", Set.of("ppt", "pptx")),

    UNSUPPORTED("unsupported", "不支持预览", Set.of());

    private final String code;
    private final String description;
    private final Set<String> supportedExtensions;

    PreviewType(String code, String description, Set<String> supportedExtensions) {
        this.code = code;
        this.description = description;
        this.supportedExtensions = supportedExtensions;
    }

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

    public boolean isPreviewable() {
        return this != UNSUPPORTED;
    }

    /**
     * 需由前端 @js-preview 等库通过下载 URL 拉取二进制后再渲染（非服务端抽取纯文本）。
     */
    public boolean requiresClientDownloadForRendering() {
        return this == PDF || this == WORD || this == EXCEL || this == PPT;
    }
}

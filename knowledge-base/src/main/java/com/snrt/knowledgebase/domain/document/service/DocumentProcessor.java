package com.snrt.knowledgebase.domain.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 文档处理器接口
 * 
 * 定义文档处理的核心方法，支持不同文件类型的处理器实现
 * 
 * @author SNRT
 * @since 1.0
 */
public interface DocumentProcessor {

    /**
     * 检查是否支持该文件类型
     * 
     * @param fileType 文件类型
     * @return 是否支持
     */
    boolean supports(String fileType);

    /**
     * 处理文档
     * 
     * @param filePath 文档文件路径
     * @param documentId 文档ID
     * @param documentName 文档名称
     * @param knowledgeBaseId 知识库ID
     * @param knowledgeBaseName 知识库名称
     */
    void processDocument(Path filePath, String documentId, String documentName,
                         String knowledgeBaseId, String knowledgeBaseName);

    /**
     * 验证文件
     * 
     * @param file 文件
     */
    void validateFile(MultipartFile file);
}

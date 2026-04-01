package com.snrt.knowledgebase.domain.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档处理器工厂
 * 
 * 根据文件类型获取对应的文档处理器实现
 * 使用策略模式，支持扩展新的文件类型处理器
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessorFactory {

    private final List<DocumentProcessor> processors;

    /**
     * 根据文件类型获取对应的处理器
     * 
     * @param fileType 文件类型
     * @return 文档处理器
     */
    public DocumentProcessor getProcessor(String fileType) {
        return processors.stream()
                .filter(processor -> processor.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的文件类型: " + fileType));
    }
}

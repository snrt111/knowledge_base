package com.snrt.knowledgebase.domain.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessorFactory {

    private final List<DocumentProcessor> processors;

    public DocumentProcessor getProcessor(String fileType) {
        return processors.stream()
                .filter(processor -> processor.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的文件类型: " + fileType));
    }
}

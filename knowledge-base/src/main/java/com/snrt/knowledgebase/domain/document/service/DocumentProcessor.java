package com.snrt.knowledgebase.domain.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface DocumentProcessor {

    boolean supports(String fileType);

    void processDocument(Path filePath, String documentId, String documentName,
                         String knowledgeBaseId, String knowledgeBaseName);

    void validateFile(MultipartFile file);
}

package com.snrt.knowledgebase.service.document.impl;

import com.snrt.knowledgebase.constants.Constants;
import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.exception.DocumentException;
import com.snrt.knowledgebase.service.DocumentProcessingService;
import com.snrt.knowledgebase.service.document.DocumentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDocumentProcessor implements DocumentProcessor {

    private final DocumentProcessingService documentProcessingService;

    @Override
    public boolean supports(String fileType) {
        return true;
    }

    @Override
    public void processDocument(Path filePath, String documentId, String documentName,
                                String knowledgeBaseId, String knowledgeBaseName) {
        documentProcessingService.processAndIndexDocument(
                filePath, documentId, documentName, knowledgeBaseId, knowledgeBaseName);
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file.getSize() > Constants.File.MAX_SIZE) {
            throw DocumentException.fileTooLarge(String.valueOf(file.getSize()));
        }
    }
}

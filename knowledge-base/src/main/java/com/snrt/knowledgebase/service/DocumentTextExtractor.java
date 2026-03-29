package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.exception.DocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 将文档流转为纯文本（Tika），供文本类预览与解析复用。
 */
@Slf4j
@Component
public class DocumentTextExtractor {

    public String extractPlainText(InputStream inputStream, String fileType) {
        try {
            DocumentReader reader = new TikaDocumentReader(new InputStreamResource(inputStream));
            List<org.springframework.ai.document.Document> documents = reader.read();

            if (documents.isEmpty()) {
                return "";
            }

            StringBuilder content = new StringBuilder();
            for (org.springframework.ai.document.Document document : documents) {
                content.append(document.getText()).append("\n");
            }

            return content.toString().trim();
        } catch (Exception e) {
            log.error("文本提取失败: fileType={}, error={}", fileType, e.getMessage(), e);
            throw DocumentException.fileReadError("文本提取失败: " + e.getMessage());
        }
    }
}

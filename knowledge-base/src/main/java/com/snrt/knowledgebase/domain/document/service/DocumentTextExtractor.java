package com.snrt.knowledgebase.domain.document.service;

import com.snrt.knowledgebase.common.exception.DocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 文档文本提取器
 * 
 * 使用Apache Tika从各种文档格式中提取纯文本内容，
 * 支持PDF、Word、Excel、PPT等多种文件格式。
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class DocumentTextExtractor {

    /**
     * 从输入流中提取纯文本内容
     * 
     * @param inputStream 文档输入流
     * @param fileType 文件类型（扩展名）
     * @return 提取的纯文本内容
     */
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

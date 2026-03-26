package com.snrt.knowledgebase.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);
    private final VectorStore vectorStore;

    public DocumentProcessingService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void processAndIndexDocument(Path filePath, String documentId, String knowledgeBaseId) {
        try {
            // 1. 读取文档内容
            List<Document> documents = readDocument(filePath);

            // 2. 为文档添加元数据
            List<Document> documentsWithMetadata = new ArrayList<>();
            for (Document doc : documents) {
                doc.getMetadata().put("documentId", documentId);
                doc.getMetadata().put("knowledgeBaseId", knowledgeBaseId);
                doc.getMetadata().put("chunkId", UUID.randomUUID().toString());
                documentsWithMetadata.add(doc);
            }

            // 3. 向量化并存储到向量数据库
            vectorStore.add(documentsWithMetadata);

            log.info("文档处理完成：{}，解析为 {} 个文档块", filePath.getFileName(), documents.size());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文档处理失败: " + e.getMessage());
        }
    }

    private List<Document> readDocument(Path filePath) throws IOException {
        List<Document> documents = new ArrayList<>();
        
        // 读取文件内容
        String content = Files.readString(filePath);
        
        // 简单的文档分块处理
        int chunkSize = 1000;
        for (int i = 0; i < content.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, content.length());
            String chunk = content.substring(i, end);
            
            Document document = new Document(chunk);
            document.getMetadata().put("filePath", filePath.toString());
            documents.add(document);
        }
        
        return documents;
    }

    public void deleteDocumentFromVectorStore(String documentId) {
        try {
            // 从向量存储中删除指定文档的所有块
            vectorStore.delete(documentId);
        } catch (Exception e) {
            e.printStackTrace();
            // 记录错误但不抛出异常，避免影响主流程
        }
    }
}

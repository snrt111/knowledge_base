package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DocumentProcessingService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    public DocumentProcessingService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.textSplitter = TokenTextSplitter.builder()
                .withChunkSize(Constants.VectorStore.CHUNK_SIZE)
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(100)
                .withMaxNumChunks(1000)
                .withKeepSeparator(true)
                .build();
    }

    public void processAndIndexDocument(Path filePath, String documentId, String documentName,
                                        String knowledgeBaseId, String knowledgeBaseName) {
        try {
            List<Document> documents = readDocument(filePath);

            List<Document> documentsWithMetadata = new ArrayList<>();
            for (Document doc : documents) {
                doc.getMetadata().put(Constants.VectorStore.METADATA_DOCUMENT_ID, documentId);
                doc.getMetadata().put(Constants.VectorStore.METADATA_DOCUMENT_NAME, documentName);
                doc.getMetadata().put(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_ID, knowledgeBaseId);
                doc.getMetadata().put(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_NAME, knowledgeBaseName);
                doc.getMetadata().put(Constants.VectorStore.METADATA_CHUNK_ID, UUID.randomUUID().toString());
                documentsWithMetadata.add(doc);
            }

            vectorStore.add(documentsWithMetadata);

            log.info("文档处理完成：{}，解析为 {} 个文档块", filePath.getFileName(), documents.size());

        } catch (Exception e) {
            log.error("文档处理失败: {}", filePath, e);
            throw new RuntimeException("文档处理失败: " + e.getMessage());
        }
    }

    private List<Document> readDocument(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".txt") || fileName.endsWith(".md") || fileName.endsWith(".json")) {
            return readTextDocument(filePath);
        }
        
        return readWithTika(filePath);
    }
    
    private List<Document> readTextDocument(Path filePath) throws IOException {
        List<Document> documents = new ArrayList<>();
        
        String content = Files.readString(filePath);
        
        int chunkSize = Constants.VectorStore.CHUNK_SIZE;
        for (int i = 0; i < content.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, content.length());
            String chunk = content.substring(i, end);
            
            Document document = new Document(chunk);
            document.getMetadata().put(Constants.VectorStore.METADATA_FILE_PATH, filePath.toString());
            documents.add(document);
        }
        
        return documents;
    }
    
    private List<Document> readWithTika(Path filePath) {
        log.info("开始使用 Tika 读取文件: {}", filePath);
        
        TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(filePath.toFile()));
        List<Document> documents = reader.get();
        
        if (documents == null || documents.isEmpty()) {
            log.warn("Tika 未能从文件中提取到任何内容: {}", filePath);
            return new ArrayList<>();
        }
        
        log.info("Tika 读取完成，原始文档数: {}, 第一个文档内容长度: {}", 
                documents.size(),
                documents.get(0).getText() != null ? documents.get(0).getText().length() : 0);
        
        List<Document> splitDocuments = textSplitter.apply(documents);
        
        if (splitDocuments == null || splitDocuments.isEmpty()) {
            log.warn("TextSplitter 分块后结果为空，使用原始文档: {}", filePath);
            splitDocuments = documents;
        }
        
        log.info("文档分块完成，分块数: {}", splitDocuments.size());
        
        for (Document doc : splitDocuments) {
            doc.getMetadata().put(Constants.VectorStore.METADATA_FILE_PATH, filePath.toString());
        }
        
        return splitDocuments;
    }

    public void deleteDocumentFromVectorStore(String documentId) {
        try {
            String filterExpression = String.format("%s in ['%s']", Constants.VectorStore.METADATA_DOCUMENT_ID, documentId);
            vectorStore.delete(filterExpression);
            log.info("从向量存储删除文档成功: documentId={}", documentId);
        } catch (Exception e) {
            log.error("从向量存储删除文档失败: documentId={}", documentId, e);
        }
    }
}

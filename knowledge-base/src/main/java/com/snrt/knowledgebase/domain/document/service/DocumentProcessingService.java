package com.snrt.knowledgebase.domain.document.service;

import com.snrt.knowledgebase.common.constants.Constants;
import com.snrt.knowledgebase.infrastructure.knowledgegraph.KnowledgeGraphSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档处理服务
 * 
 * 使用智能语义分块策略替代传统的固定大小分块
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
public class DocumentProcessingService {

    private final VectorStore vectorStore;
    private final SemanticDocumentSplitter semanticSplitter;
    private final KnowledgeGraphSyncService knowledgeGraphSyncService;

    public DocumentProcessingService(VectorStore vectorStore, SemanticDocumentSplitter semanticSplitter,
                                     KnowledgeGraphSyncService knowledgeGraphSyncService) {
        this.vectorStore = vectorStore;
        this.semanticSplitter = semanticSplitter;
        this.knowledgeGraphSyncService = knowledgeGraphSyncService;
    }

    /**
     * 处理并索引文档
     * 
     * 使用智能语义分块策略，保持语义完整性
     */
    public void processAndIndexDocument(Path filePath, String documentId, String documentName,
                                        String knowledgeBaseId, String knowledgeBaseName) {
        try {
            List<Document> documents = readDocument(filePath);

            List<Document> documentsWithMetadata = new ArrayList<>();
            for (Document doc : documents) {
                // 基础元数据
                doc.getMetadata().put(Constants.VectorStore.METADATA_DOCUMENT_ID, documentId);
                doc.getMetadata().put(Constants.VectorStore.METADATA_DOCUMENT_NAME, documentName);
                doc.getMetadata().put(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_ID, knowledgeBaseId);
                doc.getMetadata().put(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_NAME, knowledgeBaseName);
                
                // 生成唯一chunk ID（如果语义分块未生成）
                if (!doc.getMetadata().containsKey(Constants.VectorStore.METADATA_CHUNK_ID)) {
                    doc.getMetadata().put(Constants.VectorStore.METADATA_CHUNK_ID, UUID.randomUUID().toString());
                }
                
                documentsWithMetadata.add(doc);
            }

            // 批量写入向量存储
            vectorStore.add(documentsWithMetadata);

            SemanticDocumentSplitter.ChunkAnalysis analysis = semanticSplitter.analyzeChunkQuality(documents);
            log.info("文档处理完成：{}，共 {} 个语义块，平均大小 {} 字符，总字符数 {}", 
                filePath.getFileName(), 
                analysis.getChunkCount(),
                String.format("%.0f", analysis.getAvgSize()),
                analysis.getTotalSize());

            log.info("开始同步文档到知识图谱: documentId={}, knowledgeBaseId={}", documentId, knowledgeBaseId);
            knowledgeGraphSyncService.syncDocumentToKnowledgeGraph(documentId, filePath.toString(), knowledgeBaseId);
            log.info("同步文档到知识图谱完成: documentId={}, knowledgeBaseId={}", documentId, knowledgeBaseId);

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
    
    /**
     * 读取文本文件并使用智能语义分块
     */
    private List<Document> readTextDocument(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        
        // 创建原始文档
        Document originalDoc = new Document(content);
        originalDoc.getMetadata().put(Constants.VectorStore.METADATA_FILE_PATH, filePath.toString());
        
        // 使用智能语义分块
        List<Document> splitDocs = semanticSplitter.split(List.of(originalDoc));
        
        log.info("文本文件智能分块完成: {}，原始 {} 字符，生成 {} 个语义块", 
            filePath.getFileName(), content.length(), splitDocs.size());
        
        return splitDocs;
    }
    
    /**
     * 使用Tika读取文件并使用智能语义分块
     */
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
        
        // 使用智能语义分块替代传统的TokenTextSplitter
        List<Document> splitDocuments = semanticSplitter.split(documents);
        
        if (splitDocuments == null || splitDocuments.isEmpty()) {
            log.warn("智能分块后结果为空，使用原始文档: {}", filePath);
            splitDocuments = documents;
        }
        
        // 分析分块质量
        SemanticDocumentSplitter.ChunkAnalysis analysis = semanticSplitter.analyzeChunkQuality(splitDocuments);
        log.info("文档智能分块完成: {}，生成 {} 个语义块，平均大小 {} 字符", 
            filePath.getFileName(), analysis.getChunkCount(), String.format("%.0f", analysis.getAvgSize()));
        
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

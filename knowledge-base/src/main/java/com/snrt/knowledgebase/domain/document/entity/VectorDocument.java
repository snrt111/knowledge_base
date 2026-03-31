package com.snrt.knowledgebase.domain.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * 向量文档实体
 *
 * 对应 pgvector 的 vector_store 表
 *
 * @author SNRT
 * @since 1.0
 */
@Data
@Entity
@Table(name = "vector_store")
@org.hibernate.annotations.Immutable
public class VectorDocument {

    @Id
    private Long id;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @Transient
    private Double rank;

    /**
     * 获取文档ID
     */
    public String getDocumentId() {
        if (metadata != null && metadata.containsKey("document_id")) {
            return metadata.get("document_id").toString();
        }
        return null;
    }

    /**
     * 获取知识库ID
     */
    public String getKnowledgeBaseId() {
        if (metadata != null && metadata.containsKey("knowledge_base_id")) {
            return metadata.get("knowledge_base_id").toString();
        }
        return null;
    }

    /**
     * 获取文档名称
     */
    public String getDocumentName() {
        if (metadata != null && metadata.containsKey("document_name")) {
            return metadata.get("document_name").toString();
        }
        return "未知文档";
    }

    /**
     * 获取知识库名称
     */
    public String getKnowledgeBaseName() {
        if (metadata != null && metadata.containsKey("knowledge_base_name")) {
            return metadata.get("knowledge_base_name").toString();
        }
        return "未知知识库";
    }
}

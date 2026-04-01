package com.snrt.knowledgebase.domain.knowledgegraph.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDateTime;

/**
 * 知识图谱实体
 * 
 * 对应Neo4j图数据库中的知识图谱节点
 * 包含知识图谱的基本信息和关联的知识库ID
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Node("KnowledgeGraph")
public class KnowledgeGraph {

    @Id
    private String uuid;

    @Property("name")
    private String name;

    @Property("description")
    private String description;

    @Property("knowledge_base_id")
    private String knowledgeBaseId;

    @Property("create_time")
    private LocalDateTime createTime;

    @Property("update_time")
    private LocalDateTime updateTime;

    @Property("is_deleted")
    private Boolean isDeleted = false;
}

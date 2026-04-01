package com.snrt.knowledgebase.domain.knowledgegraph.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDateTime;

/**
 * 知识图谱关系实体
 * 
 * 对应Neo4j图数据库中的关系
 * 包含关系类型、属性、关联的节点等信息
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class KnowledgeGraphRelationEntity {

    @Id
    private String uuid;

    @Property("type")
    private String type;

    @Property("properties")
    private String properties;

    @Property("knowledge_graph_uuid")
    private String knowledgeGraphUuid;

    @Property("create_time")
    private LocalDateTime createTime;

    @Property("update_time")
    private LocalDateTime updateTime;

    @Property("is_deleted")
    private Boolean isDeleted = false;

    @Property("from_node_uuid")
    private String fromNodeUuid;

    @Property("to_node_uuid")
    private String toNodeUuid;
}

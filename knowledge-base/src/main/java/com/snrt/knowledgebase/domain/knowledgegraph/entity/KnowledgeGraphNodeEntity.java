package com.snrt.knowledgebase.domain.knowledgegraph.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDateTime;

@Data
@Node("KGNode")
public class KnowledgeGraphNodeEntity {

    @Id
    private String uuid;

    @Property("label")
    private String label;

    @Property("name")
    private String name;

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
}

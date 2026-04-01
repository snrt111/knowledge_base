package com.snrt.knowledgebase.common.exception;

import lombok.Getter;

@Getter
public class KnowledgeGraphException extends BusinessException {

    public KnowledgeGraphException(ErrorCode errorCode) {
        super(errorCode);
    }

    public KnowledgeGraphException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static KnowledgeGraphException notFound(String id) {
        return new KnowledgeGraphException(ErrorCode.KNOWLEDGE_GRAPH_NODE_NOT_FOUND, "知识图谱节点不存在: " + id);
    }

    public static KnowledgeGraphException alreadyExists(String name) {
        return new KnowledgeGraphException(ErrorCode.KNOWLEDGE_GRAPH_NODE_ALREADY_EXISTS, "知识图谱节点已存在: " + name);
    }

    public static KnowledgeGraphException invalidRelation(String type) {
        return new KnowledgeGraphException(ErrorCode.PARAM_ERROR, "无效的关系类型: " + type);
    }
}

package com.snrt.knowledgebase.common.exception;

import lombok.Getter;

/**
 * 知识图谱异常
 * 
 * 用于表示知识图谱相关的业务异常
 * 提供静态工厂方法方便创建各种知识图谱异常
 * 
 * @author SNRT
 * @since 1.0
 */
@Getter
public class KnowledgeGraphException extends BusinessException {

    /**
     * 构造知识图谱异常
     * 
     * @param errorCode 错误码
     */
    public KnowledgeGraphException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 构造知识图谱异常
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public KnowledgeGraphException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 创建知识图谱节点不存在异常
     * 
     * @param id 节点ID
     * @return KnowledgeGraphException实例
     */
    public static KnowledgeGraphException notFound(String id) {
        return new KnowledgeGraphException(ErrorCode.KNOWLEDGE_GRAPH_NODE_NOT_FOUND, "知识图谱节点不存在: " + id);
    }

    /**
     * 创建知识图谱节点已存在异常
     * 
     * @param name 节点名称
     * @return KnowledgeGraphException实例
     */
    public static KnowledgeGraphException alreadyExists(String name) {
        return new KnowledgeGraphException(ErrorCode.KNOWLEDGE_GRAPH_NODE_ALREADY_EXISTS, "知识图谱节点已存在: " + name);
    }

    /**
     * 创建无效关系类型异常
     * 
     * @param type 关系类型
     * @return KnowledgeGraphException实例
     */
    public static KnowledgeGraphException invalidRelation(String type) {
        return new KnowledgeGraphException(ErrorCode.PARAM_ERROR, "无效的关系类型: " + type);
    }
}

package com.snrt.knowledgebase.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 
 * 统一定义系统中所有错误码：
 * - 系统错误
 * - 参数错误
 * - 资源错误
 * - 文档错误
 * - 知识库错误
 * - 知识图谱错误
 * - 聊天错误
 * - 外部服务错误
 * 
 * @author SNRT
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    /**
     * 成功
     */
    SUCCESS(200, "success"),

    /**
     * 系统错误
     */
    SYSTEM_ERROR(1000, "系统错误，请稍后重试"),
    /**
     * 参数错误
     */
    PARAM_ERROR(1001, "参数错误: {0}"),
    /**
     * 数据校验失败
     */
    VALIDATION_ERROR(1002, "数据校验失败: {0}"),

    /**
     * 资源不存在
     */
    RESOURCE_NOT_FOUND(2000, "{0}不存在: {1}"),
    /**
     * 资源已存在
     */
    RESOURCE_ALREADY_EXISTS(2001, "{0}已存在: {1}"),

    /**
     * 文档上传失败
     */
    DOCUMENT_UPLOAD_FAILED(3000, "文档上传失败: {0}"),
    /**
     * 文档处理失败
     */
    DOCUMENT_PROCESS_FAILED(3001, "文档处理失败: {0}"),
    /**
     * 文档不存在
     */
    DOCUMENT_NOT_FOUND(3002, "文档不存在: {0}"),
    /**
     * 不支持的文件类型
     */
    UNSUPPORTED_FILE_TYPE(3003, "不支持的文件类型: {0}"),
    /**
     * 文件大小超过限制
     */
    FILE_TOO_LARGE(3004, "文件大小超过限制: {0}"),
    /**
     * 文件读取失败
     */
    FILE_READ_ERROR(3005, "文件读取失败: {0}"),
    /**
     * 不允许重新处理
     */
    DOCUMENT_REPROCESS_NOT_ALLOWED(3006, "{0}"),

    /**
     * 知识库不存在
     */
    KNOWLEDGE_BASE_NOT_FOUND(4000, "知识库不存在: {0}"),
    /**
     * 知识库已存在
     */
    KNOWLEDGE_BASE_ALREADY_EXISTS(4001, "知识库已存在: {0}"),

    /**
     * 知识图谱不存在
     */
    KNOWLEDGE_GRAPH_NOT_FOUND(4100, "知识图谱不存在: {0}"),
    /**
     * 知识图谱节点不存在
     */
    KNOWLEDGE_GRAPH_NODE_NOT_FOUND(4101, "知识图谱节点不存在: {0}"),
    /**
     * 知识图谱关系不存在
     */
    KNOWLEDGE_GRAPH_RELATION_NOT_FOUND(4102, "知识图谱关系不存在: {0}"),
    /**
     * 知识图谱节点已存在
     */
    KNOWLEDGE_GRAPH_NODE_ALREADY_EXISTS(4103, "知识图谱节点已存在: {0}"),

    /**
     * 会话不存在
     */
    CHAT_SESSION_NOT_FOUND(5000, "会话不存在: {0}"),
    /**
     * 模型调用失败
     */
    MODEL_CALL_FAILED(5001, "模型调用失败: {0}"),
    /**
     * 消息内容无效
     */
    INVALID_MESSAGE(5002, "消息内容无效: {0}"),

    /**
     * MinIO操作失败
     */
    MINIO_ERROR(6000, "MinIO操作失败: {0}"),
    /**
     * 向量存储操作失败
     */
    VECTOR_STORE_ERROR(6001, "向量存储操作失败: {0}"),
    /**
     * 外部服务调用失败
     */
    EXTERNAL_SERVICE_ERROR(6002, "外部服务调用失败: {0}");

    private final int code;
    private final String message;
}

package com.snrt.knowledgebase.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "success"),

    SYSTEM_ERROR(1000, "系统错误，请稍后重试"),
    PARAM_ERROR(1001, "参数错误: {0}"),
    VALIDATION_ERROR(1002, "数据校验失败: {0}"),

    RESOURCE_NOT_FOUND(2000, "{0}不存在: {1}"),
    RESOURCE_ALREADY_EXISTS(2001, "{0}已存在: {1}"),

    DOCUMENT_UPLOAD_FAILED(3000, "文档上传失败: {0}"),
    DOCUMENT_PROCESS_FAILED(3001, "文档处理失败: {0}"),
    DOCUMENT_NOT_FOUND(3002, "文档不存在: {0}"),
    UNSUPPORTED_FILE_TYPE(3003, "不支持的文件类型: {0}"),
    FILE_TOO_LARGE(3004, "文件大小超过限制: {0}"),
    FILE_READ_ERROR(3005, "文件读取失败: {0}"),

    KNOWLEDGE_BASE_NOT_FOUND(4000, "知识库不存在: {0}"),
    KNOWLEDGE_BASE_ALREADY_EXISTS(4001, "知识库已存在: {0}"),

    CHAT_SESSION_NOT_FOUND(5000, "会话不存在: {0}"),
    MODEL_CALL_FAILED(5001, "模型调用失败: {0}"),
    INVALID_MESSAGE(5002, "消息内容无效: {0}"),

    MINIO_ERROR(6000, "MinIO操作失败: {0}"),
    VECTOR_STORE_ERROR(6001, "向量存储操作失败: {0}"),
    EXTERNAL_SERVICE_ERROR(6002, "外部服务调用失败: {0}");

    private final int code;
    private final String message;
}

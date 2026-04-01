package com.snrt.knowledgebase.common.exception;

/**
 * 文档异常
 * 
 * 用于表示文档相关的业务异常
 * 提供静态工厂方法方便创建各种文档异常
 * 
 * @author SNRT
 * @since 1.0
 */
public class DocumentException extends BusinessException {

    /**
     * 构造文档异常
     * 
     * @param errorCode 错误码
     * @param args 错误参数
     */
    public DocumentException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    /**
     * 构造文档异常（带异常原因）
     * 
     * @param errorCode 错误码
     * @param cause 异常原因
     * @param args 错误参数
     */
    public DocumentException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    /**
     * 创建文档上传失败异常
     * 
     * @param message 错误消息
     * @return DocumentException实例
     */
    public static DocumentException uploadFailed(String message) {
        return new DocumentException(ErrorCode.DOCUMENT_UPLOAD_FAILED, message);
    }

    /**
     * 创建文档上传失败异常（带异常原因）
     * 
     * @param message 错误消息
     * @param cause 异常原因
     * @return DocumentException实例
     */
    public static DocumentException uploadFailed(String message, Throwable cause) {
        return new DocumentException(ErrorCode.DOCUMENT_UPLOAD_FAILED, cause, message);
    }

    /**
     * 创建文档处理失败异常
     * 
     * @param message 错误消息
     * @return DocumentException实例
     */
    public static DocumentException processFailed(String message) {
        return new DocumentException(ErrorCode.DOCUMENT_PROCESS_FAILED, message);
    }

    /**
     * 创建文档处理失败异常（带异常原因）
     * 
     * @param message 错误消息
     * @param cause 异常原因
     * @return DocumentException实例
     */
    public static DocumentException processFailed(String message, Throwable cause) {
        return new DocumentException(ErrorCode.DOCUMENT_PROCESS_FAILED, cause, message);
    }

    /**
     * 创建文档不存在异常
     * 
     * @param id 文档ID
     * @return DocumentException实例
     */
    public static DocumentException notFound(String id) {
        return new DocumentException(ErrorCode.DOCUMENT_NOT_FOUND, id);
    }

    /**
     * 创建不支持的文件类型异常
     * 
     * @param type 文件类型
     * @return DocumentException实例
     */
    public static DocumentException unsupportedType(String type) {
        return new DocumentException(ErrorCode.UNSUPPORTED_FILE_TYPE, type);
    }

    /**
     * 创建文件大小超限异常
     * 
     * @param size 文件大小
     * @return DocumentException实例
     */
    public static DocumentException fileTooLarge(String size) {
        return new DocumentException(ErrorCode.FILE_TOO_LARGE, size);
    }

    /**
     * 创建文件读取失败异常
     * 
     * @param message 错误消息
     * @return DocumentException实例
     */
    public static DocumentException fileReadError(String message) {
        return new DocumentException(ErrorCode.FILE_READ_ERROR, message);
    }

    /**
     * 创建不允许重新处理异常
     * 
     * @param message 错误消息
     * @return DocumentException实例
     */
    public static DocumentException reprocessNotAllowed(String message) {
        return new DocumentException(ErrorCode.DOCUMENT_REPROCESS_NOT_ALLOWED, message);
    }
}

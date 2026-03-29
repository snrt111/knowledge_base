package com.snrt.knowledgebase.exception;

public class DocumentException extends BusinessException {

    public DocumentException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public DocumentException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    public static DocumentException uploadFailed(String message) {
        return new DocumentException(ErrorCode.DOCUMENT_UPLOAD_FAILED, message);
    }

    public static DocumentException uploadFailed(String message, Throwable cause) {
        return new DocumentException(ErrorCode.DOCUMENT_UPLOAD_FAILED, cause, message);
    }

    public static DocumentException processFailed(String message) {
        return new DocumentException(ErrorCode.DOCUMENT_PROCESS_FAILED, message);
    }

    public static DocumentException processFailed(String message, Throwable cause) {
        return new DocumentException(ErrorCode.DOCUMENT_PROCESS_FAILED, cause, message);
    }

    public static DocumentException notFound(String id) {
        return new DocumentException(ErrorCode.DOCUMENT_NOT_FOUND, id);
    }

    public static DocumentException unsupportedType(String type) {
        return new DocumentException(ErrorCode.UNSUPPORTED_FILE_TYPE, type);
    }

    public static DocumentException fileTooLarge(String size) {
        return new DocumentException(ErrorCode.FILE_TOO_LARGE, size);
    }

    public static DocumentException fileReadError(String message) {
        return new DocumentException(ErrorCode.FILE_READ_ERROR, message);
    }

    public static DocumentException reprocessNotAllowed(String message) {
        return new DocumentException(ErrorCode.DOCUMENT_REPROCESS_NOT_ALLOWED, message);
    }
}

package com.snrt.knowledgebase.common.exception;

import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.exception.impl.DefaultExceptionLogger;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Hidden
public class GlobalExceptionHandler {

    private final DefaultExceptionLogger exceptionLogger;

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(ValidationException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(DocumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDocumentException(DocumentException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleExternalServiceException(ExternalServiceException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ExceptionContext context = buildExceptionContext(request, e);
        context.setParams(e.getBindingResult().getTarget());
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.PARAM_ERROR, message));
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.VALIDATION_ERROR, message));
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = "缺少必要参数: " + e.getParameterName();
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.PARAM_ERROR, message));
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("参数类型错误: %s 应为 %s", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型");
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.PARAM_ERROR, message));
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        String message = "文件大小超过限制";
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.FILE_TOO_LARGE, message));
        return ApiResponse.error(ErrorCode.FILE_TOO_LARGE.getCode(), message);
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "数据库操作失败，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.PARAM_ERROR, e.getMessage()));
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误，请稍后重试");
    }

    private ExceptionContext buildExceptionContext(HttpServletRequest request, Throwable throwable) {
        return ExceptionContext.builder()
                .traceId(request.getAttribute("traceId") != null ? request.getAttribute("traceId").toString() : "")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .exceptionType(throwable.getClass().getSimpleName())
                .message(throwable.getMessage())
                .build();
    }
}

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

/**
 * 全局异常处理器
 * 
 * 统一处理系统中的各种异常：
 * - 业务异常（BusinessException）
 * - 资源未找到异常（ResourceNotFoundException）
 * - 参数校验异常（ValidationException、MethodArgumentNotValidException等）
 * - 系统异常（DataAccessException、RuntimeException等）
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Hidden
public class GlobalExceptionHandler {

    private final DefaultExceptionLogger exceptionLogger;

    /**
     * 处理业务异常
     * 
     * @param e 业务异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 处理资源未找到异常
     * 
     * @param e 资源未找到异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     * 
     * @param e 参数校验异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(ValidationException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 处理文档相关异常
     * 
     * @param e 文档异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(DocumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDocumentException(DocumentException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 处理知识图谱异常
     * 
     * @param e 知识图谱异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(KnowledgeGraphException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleKnowledgeGraphException(KnowledgeGraphException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 处理外部服务异常
     * 
     * @param e 外部服务异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(ExternalServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleExternalServiceException(ExternalServiceException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 处理参数校验失败异常
     * 
     * @param e 参数校验失败异常
     * @param request HTTP请求
     * @return 统一响应
     */
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

    /**
     * 处理约束违反异常
     * 
     * @param e 约束违反异常
     * @param request HTTP请求
     * @return 统一响应
     */
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

    /**
     * 处理缺少请求参数异常
     * 
     * @param e 缺少请求参数异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = "缺少必要参数: " + e.getParameterName();
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.PARAM_ERROR, message));
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理参数类型不匹配异常
     * 
     * @param e 参数类型不匹配异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("参数类型错误: %s 应为 %s", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型");
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.PARAM_ERROR, message));
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理文件大小超限异常
     * 
     * @param e 文件大小超限异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        String message = "文件大小超过限制";
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.FILE_TOO_LARGE, message));
        return ApiResponse.error(ErrorCode.FILE_TOO_LARGE.getCode(), message);
    }

    /**
     * 处理数据访问异常
     * 
     * @param e 数据访问异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "数据库操作失败，请稍后重试");
    }

    /**
     * 处理参数非法异常
     * 
     * @param e 参数非法异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logBusinessException(context, new BusinessException(ErrorCode.PARAM_ERROR, e.getMessage()));
        return ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理非法状态异常
     * 
     * @param e 非法状态异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常
     * 
     * @param e 运行时异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误，请稍后重试");
    }

    /**
     * 处理其他异常
     * 
     * @param e 其他异常
     * @param request HTTP请求
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        ExceptionContext context = buildExceptionContext(request, e);
        exceptionLogger.logSystemException(context, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误，请稍后重试");
    }

    /**
     * 构建异常上下文
     * 
     * @param request HTTP请求
     * @param throwable 异常对象
     * @return 异常上下文
     */
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

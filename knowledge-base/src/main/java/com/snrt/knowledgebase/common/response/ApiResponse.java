package com.snrt.knowledgebase.common.response;

import lombok.Data;

/**
 * 统一API响应封装
 * 
 * 用于统一后端API的响应格式
 * 包含状态码、消息、数据和时间戳
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应
     * 
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 创建空成功响应
     * 
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 创建错误响应
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建自定义错误响应
     * 
     * @param code 状态码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}

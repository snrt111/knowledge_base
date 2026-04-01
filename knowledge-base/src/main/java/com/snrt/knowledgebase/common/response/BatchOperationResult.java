package com.snrt.knowledgebase.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果封装
 * 
 * 用于封装批量操作的执行结果
 * 包含成功和失败的条目列表以及统计信息
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult<T> {

    private int total;

    private int success;

    private int failed;

    @Builder.Default
    private List<T> successItems = new ArrayList<>();

    @Builder.Default
    private List<FailedItem> failedItems = new ArrayList<>();

    /**
     * 创建空的批量操作结果
     * 
     * @param <T> 数据类型
     * @return BatchOperationResult实例
     */
    public static <T> BatchOperationResult<T> empty() {
        return BatchOperationResult.<T>builder()
                .total(0)
                .success(0)
                .failed(0)
                .successItems(new ArrayList<>())
                .failedItems(new ArrayList<>())
                .build();
    }

    /**
     * 失败条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private String id;
        private String reason;
    }
}

package com.snrt.knowledgebase.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private String id;
        private String reason;
    }

    public static <T> BatchOperationResult<T> empty() {
        return BatchOperationResult.<T>builder()
                .total(0)
                .success(0)
                .failed(0)
                .successItems(new ArrayList<>())
                .failedItems(new ArrayList<>())
                .build();
    }
}

package com.snrt.knowledgebase.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDocumentRequest {

    @NotEmpty(message = "文档ID列表不能为空")
    private List<String> documentIds;
}

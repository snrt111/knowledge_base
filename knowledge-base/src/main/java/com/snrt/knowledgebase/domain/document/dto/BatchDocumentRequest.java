package com.snrt.knowledgebase.domain.document.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量文档请求DTO
 * 
 * 包含批量操作所需的文档ID列表
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class BatchDocumentRequest {

    @NotEmpty(message = "文档ID列表不能为空")
    private List<String> documentIds;
}

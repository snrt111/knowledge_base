package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新知识图谱请求DTO
 * 
 * 包含知识图谱的名称、描述信息
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class UpdateKnowledgeGraphRequest {

    @Size(max = 100, message = "知识图谱名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}

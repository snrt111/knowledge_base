package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateKnowledgeGraphRequest {

    @Size(max = 100, message = "知识图谱名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}

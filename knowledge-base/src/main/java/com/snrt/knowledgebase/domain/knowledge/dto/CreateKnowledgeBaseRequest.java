package com.snrt.knowledgebase.domain.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(min = 1, max = 100, message = "知识库名称长度必须在1-100个字符之间")
    private String name;

    @Size(max = 500, message = "知识库描述长度不能超过500个字符")
    private String description;
}

package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeGraphRelationCreateRequest {

    @NotBlank(message = "关系类型不能为空")
    @Size(max = 50, message = "关系类型长度不能超过50个字符")
    private String type;

    @NotBlank(message = "起始节点ID不能为空")
    private String fromNodeId;

    @NotBlank(message = "目标节点ID不能为空")
    private String toNodeId;

    private String properties;
}

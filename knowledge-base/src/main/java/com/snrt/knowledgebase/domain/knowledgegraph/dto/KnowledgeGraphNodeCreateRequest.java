package com.snrt.knowledgebase.domain.knowledgegraph.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeGraphNodeCreateRequest {

    @NotBlank(message = "节点标签不能为空")
    @Size(max = 100, message = "标签长度不能超过100个字符")
    private String label;

    @NotBlank(message = "节点名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    private String name;

    private String properties;
}

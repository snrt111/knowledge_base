package com.snrt.knowledgebase.domain.chat.dto;

import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提示词结果DTO
 * 
 * 包含构建好的提示词和引用的文档来源列表
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResult {

    private String prompt;

    private List<DocumentSourceDTO> documentSources;
}

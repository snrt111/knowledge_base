package com.snrt.knowledgebase.domain.chat.dto;

import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResult {

    private String prompt;

    private List<DocumentSourceDTO> documentSources;
}

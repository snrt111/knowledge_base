package com.snrt.knowledgebase.domain.chat.dto;

import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流式聊天响应DTO
 * 用于SSE流式返回AI回答内容和文档来源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamChatResponse {

    /**
     * 响应类型：content-内容块，sources-文档来源，complete-完成标记
     */
    private String type;

    /**
     * 内容（当type为content时）
     */
    private String content;

    /**
     * 文档来源列表（当type为sources时）
     */
    private List<DocumentSourceDTO> sources;

    /**
     * 创建内容类型的响应
     */
    public static StreamChatResponse content(String content) {
        return StreamChatResponse.builder()
                .type("content")
                .content(content)
                .build();
    }

    /**
     * 创建文档来源类型的响应
     */
    public static StreamChatResponse sources(List<DocumentSourceDTO> sources) {
        return StreamChatResponse.builder()
                .type("sources")
                .sources(sources)
                .build();
    }

    /**
     * 创建完成标记类型的响应
     */
    public static StreamChatResponse complete() {
        return StreamChatResponse.builder()
                .type("complete")
                .build();
    }
}

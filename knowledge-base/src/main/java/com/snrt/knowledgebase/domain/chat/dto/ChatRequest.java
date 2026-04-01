package com.snrt.knowledgebase.domain.chat.dto;

import com.snrt.knowledgebase.domain.chat.constants.ChatConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求DTO
 * 
 * 包含用户消息、会话ID、知识库ID和流式响应标志
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = ChatConstants.Message.MAX_CONTENT_LENGTH, message = "消息内容长度不能超过" + ChatConstants.Message.MAX_CONTENT_LENGTH + "个字符")
    private String message;

    private String sessionId;

    private String knowledgeBaseId;

    /**
     * 是否启用流式响应
     * 
     * @return true为流式，false为同步
     */
    private Boolean stream = false;
}

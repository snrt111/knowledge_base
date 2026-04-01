package com.snrt.knowledgebase.domain.chat.dto;

import com.snrt.knowledgebase.domain.chat.constants.ChatConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = ChatConstants.Message.MAX_CONTENT_LENGTH, message = "消息内容长度不能超过" + ChatConstants.Message.MAX_CONTENT_LENGTH + "个字符")
    private String message;

    private String sessionId;

    private String knowledgeBaseId;

    private Boolean stream = false;
}

package com.snrt.knowledgebase.domain.chat.dto;

import com.snrt.knowledgebase.domain.chat.constants.ChatConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChatSessionRequest {

    @Size(max = ChatConstants.Session.MAX_TITLE_LENGTH, message = "会话标题长度不能超过" + ChatConstants.Session.MAX_TITLE_LENGTH + "个字符")
    private String title;

    private String knowledgeBaseId;
}

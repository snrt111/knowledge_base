package com.snrt.knowledgebase.domain.chat.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChatSessionRequest {

    @Size(max = 100, message = "会话标题长度不能超过100个字符")
    private String title;

    private String knowledgeBaseId;
}

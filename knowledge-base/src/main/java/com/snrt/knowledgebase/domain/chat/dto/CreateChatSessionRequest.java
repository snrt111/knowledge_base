package com.snrt.knowledgebase.domain.chat.dto;

import com.snrt.knowledgebase.domain.chat.constants.ChatConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建聊天会话请求DTO
 * 
 * 包含会话标题和关联的知识库ID
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class CreateChatSessionRequest {

    @Size(max = ChatConstants.Session.MAX_TITLE_LENGTH, message = "会话标题长度不能超过" + ChatConstants.Session.MAX_TITLE_LENGTH + "个字符")
    private String title;

    /**
     * 知识库ID
     */
    private String knowledgeBaseId;
}

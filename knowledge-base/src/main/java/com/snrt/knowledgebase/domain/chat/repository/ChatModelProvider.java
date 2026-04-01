package com.snrt.knowledgebase.domain.chat.repository;

import org.springframework.ai.chat.model.ChatModel;

/**
 * Chat模型提供商接口
 * 
 * 定义了Chat模型提供商的核心方法，支持不同的AI模型提供商实现
 * 
 * @author SNRT
 * @since 1.0
 */
public interface ChatModelProvider {

    /**
     * 获取提供商名称
     * 
     * @return 提供商名称
     */
    String getProviderName();

    /**
     * 创建Chat模型实例
     * 
     * @return Chat模型实例
     */
    ChatModel createModel();
}

package com.snrt.knowledgebase.domain.chat.repository;

import com.snrt.knowledgebase.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天消息Repository
 * 
 * 提供聊天消息的CRUD操作和查询功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    /**
     * 根据会话ID查询消息列表，按创建时间升序
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    /**
     * 根据会话ID删除消息
     * 
     * @param sessionId 会话ID
     */
    void deleteBySessionId(String sessionId);

    /**
     * 根据会话ID统计消息数量
     * 
     * @param sessionId 会话ID
     * @return 消息数量
     */
    long countBySessionId(String sessionId);
}

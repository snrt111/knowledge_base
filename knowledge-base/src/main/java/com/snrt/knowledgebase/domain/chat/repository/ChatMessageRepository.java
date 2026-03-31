package com.snrt.knowledgebase.domain.chat.repository;

import com.snrt.knowledgebase.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    void deleteBySessionId(String sessionId);

    long countBySessionId(String sessionId);
}

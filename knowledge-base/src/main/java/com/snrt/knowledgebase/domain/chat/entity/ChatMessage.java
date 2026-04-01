package com.snrt.knowledgebase.domain.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 * 
 * 对应数据库中的聊天消息表
 * 包含消息内容、角色（用户/助手/系统）和关联的会话
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * AI回答引用的文档来源信息，以JSON格式存储
     */
    @Column(name = "document_sources", columnDefinition = "TEXT")
    private String documentSources;

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }
}

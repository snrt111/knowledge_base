package com.snrt.knowledgebase.domain.chat.entity;

import com.snrt.knowledgebase.domain.knowledge.entity.KnowledgeBase;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 100)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id")
    private KnowledgeBase knowledgeBase;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createTime ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}

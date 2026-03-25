package com.snrt.knowledgebase.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {

    private String id;
    private String role;
    private String content;
    private LocalDateTime createTime;
}

package com.snrt.knowledgebase.dto;

import lombok.Data;

@Data
public class ChatRequest {

    private String message;
    private String sessionId;
    private String knowledgeBaseId;
    private Boolean stream = false;
}

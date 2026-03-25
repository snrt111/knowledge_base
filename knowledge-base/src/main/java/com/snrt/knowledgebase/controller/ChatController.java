package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.dto.*;
import com.snrt.knowledgebase.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/sessions")
    public ApiResponse<PageResult<ChatSessionDTO>> listSessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(chatService.listSessions(page, size, keyword));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<ChatSessionDTO> getSession(@PathVariable String id) {
        return ApiResponse.success(chatService.getSession(id));
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageDTO>> getSessionMessages(@PathVariable String id) {
        return ApiResponse.success(chatService.getSessionMessages(id));
    }

    @PostMapping("/sessions")
    public ApiResponse<ChatSessionDTO> createSession(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String knowledgeBaseId = request.get("knowledgeBaseId");
        return ApiResponse.success(chatService.createSession(title, knowledgeBaseId));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable String id) {
        chatService.deleteSession(id);
        return ApiResponse.success();
    }

    @PostMapping
    public ApiResponse<String> chat(@RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.chat(request));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String knowledgeBaseId) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);
        request.setKnowledgeBaseId(knowledgeBaseId);
        return chatService.streamChat(request);
    }
}

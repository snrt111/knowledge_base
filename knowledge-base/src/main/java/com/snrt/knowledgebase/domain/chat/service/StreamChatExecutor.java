package com.snrt.knowledgebase.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.PromptResult;
import com.snrt.knowledgebase.domain.chat.dto.StreamChatResponse;
import com.snrt.knowledgebase.domain.chat.entity.ChatMessage;
import com.snrt.knowledgebase.domain.chat.entity.ChatSession;
import com.snrt.knowledgebase.domain.chat.repository.ChatModelFactory;
import com.snrt.knowledgebase.domain.chat.repository.ChatMessageRepository;
import com.snrt.knowledgebase.domain.chat.repository.ChatSessionRepository;
import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import com.snrt.knowledgebase.domain.document.service.ConversationSummarizer;
import com.snrt.knowledgebase.common.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamChatExecutor {

    private final ChatModelFactory chatModelFactory;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ConversationSummarizer conversationSummarizer;
    private final RAGPromptBuilder ragPromptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Flux<StreamChatResponse> executeStreamChat(ChatRequest request) {
        Instant start = Instant.now();
        String traceId = LogUtils.getTraceId();

        ChatSession session = findOrCreateSession(request);

        log.info("[{}] [大模型流式请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                traceId, session.getId(), request.getKnowledgeBaseId(), request.getMessage());

        saveUserMessage(session.getId(), request.getMessage());

        String knowledgeBaseId = getKnowledgeBaseId(session);
        String compressedContext = compressContext(session.getId());

        PromptResult promptResult = ragPromptBuilder.build(request.getMessage(), knowledgeBaseId, compressedContext);
        log.debug("[{}] [大模型流式处理] 会话ID: {}, 构建的Prompt长度: {} 字符",
                traceId, session.getId(), promptResult.getPrompt().length());

        return executeModelStream(session, promptResult, start, traceId);
    }

    private Flux<StreamChatResponse> executeModelStream(ChatSession session, PromptResult promptResult, 
                                                        Instant start, String traceId) {
        ChatModel chatModel = chatModelFactory.getDefaultModel();
        StringBuilder fullResponse = new StringBuilder();

        StreamChatResponse sourcesResponse = StreamChatResponse.sources(promptResult.getDocumentSources());

        return chatModel.stream(promptResult.getPrompt())
                .map(StreamChatResponse::content)
                .doOnNext(response -> {
                    if (response.getContent() != null) {
                        fullResponse.append(response.getContent());
                        log.debug("[{}] [大模型流式响应] 会话ID: {}, 接收数据块长度: {}",
                                traceId, session.getId(), response.getContent().length());
                    }
                })
                .startWith(sourcesResponse)
                .concatWithValues(StreamChatResponse.complete())
                .doOnComplete(() -> handleCompletion(session, promptResult, fullResponse, start, traceId))
                .doOnError(error -> handleError(session, start, traceId, error));
    }

    private ChatSession findOrCreateSession(ChatRequest request) {
        return sessionRepository.findByIdAndIsDeletedFalse(request.getSessionId())
                .orElseGet(() -> createNewSession(request));
    }

    private ChatSession createNewSession(ChatRequest request) {
        String title = generateSessionTitle(request.getMessage());

        ChatSession session = new ChatSession();
        session.setTitle(title);

        if (request.getKnowledgeBaseId() != null && !request.getKnowledgeBaseId().isEmpty()) {
            var kb = sessionRepository.findByIdAndIsDeletedFalse(request.getKnowledgeBaseId())
                    .map(chatSession -> chatSession.getKnowledgeBase())
                    .orElse(null);
            session.setKnowledgeBase(kb);
        }

        return sessionRepository.save(session);
    }

    private String generateSessionTitle(String message) {
        if (message != null && message.length() > 20) {
            return message.substring(0, 20) + "...";
        }
        return message;
    }

    private void saveUserMessage(String sessionId, String message) {
        saveMessage(sessionId, ChatMessage.MessageRole.USER, message, null);
    }

    private void saveAssistantMessage(String sessionId, String response, List<DocumentSourceDTO> sources) {
        String sourcesJson = convertToJson(sources);
        saveMessage(sessionId, ChatMessage.MessageRole.ASSISTANT, response, sourcesJson);
    }

    private void saveMessage(String sessionId, ChatMessage.MessageRole role, String content, String sources) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setDocumentSources(sources);
        messageRepository.save(message);
    }

    private String getKnowledgeBaseId(ChatSession session) {
        return session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
    }

    private String compressContext(String sessionId) {
        List<ChatMessage> historyMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        return conversationSummarizer.compressContext(historyMessages);
    }

    private String convertToJson(List<DocumentSourceDTO> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            log.warn("转换文档来源为JSON失败", e);
            return null;
        }
    }

    private void handleCompletion(ChatSession session, PromptResult promptResult, 
                                  StringBuilder fullResponse, Instant start, String traceId) {
        Duration duration = Duration.between(start, Instant.now());
        LogUtils.logPerformance("streamChat", duration, 10000);

        log.info("[{}] [大模型流式回答完成] 会话ID: {}, 响应耗时: {}ms, 完整回答长度: {} 字符",
                traceId, session.getId(), duration.toMillis(), fullResponse.length());
        saveAssistantMessage(session.getId(), fullResponse.toString(), promptResult.getDocumentSources());
    }

    private void handleError(ChatSession session, Instant start, String traceId, Throwable error) {
        Duration duration = Duration.between(start, Instant.now());
        log.error("[{}] [大模型流式错误] 会话ID: {}, 耗时: {}ms, 错误信息: {}",
                traceId, session.getId(), duration.toMillis(), error.getMessage(), error);
    }
}

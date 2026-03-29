package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConversationSummarizer {

    private static final int SUMMARIZE_THRESHOLD = 6;
    private static final int SUMMARY_MAX_LENGTH = 200;
    private static final int KEEP_RECENT_MESSAGES = 2;

    private final ChatModel chatModel;

    public ConversationSummarizer(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 压缩对话历史
     * 当历史消息超过阈值时，使用轻量级模型生成摘要
     *
     * @param messages 原始消息列表（包含当前用户消息）
     * @return 压缩后的上下文字符串，如果消息数量较少则返回空字符串
     */
    public String compressContext(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        // 排除最后一条消息（当前用户消息），只处理历史消息
        List<ChatMessage> historyMessages = messages.size() > 1
                ? messages.subList(0, messages.size() - 1)
                : new ArrayList<>();

        if (historyMessages.isEmpty()) {
            return "";
        }

        // 消息数量较少时，直接格式化返回
        if (historyMessages.size() <= SUMMARIZE_THRESHOLD) {
            return formatMessages(historyMessages);
        }

        log.info("[上下文压缩] 历史消息数: {}, 触发摘要生成", historyMessages.size());

        // 1. 早期消息生成摘要
        List<ChatMessage> earlyMessages = historyMessages.subList(0, historyMessages.size() - KEEP_RECENT_MESSAGES);
        String summary = generateSummary(earlyMessages);

        // 2. 保留最近N轮完整对话
        List<ChatMessage> recentMessages = historyMessages.subList(historyMessages.size() - KEEP_RECENT_MESSAGES, historyMessages.size());
        String recentContext = formatMessages(recentMessages);

        String result = String.format("""
            【历史摘要】%s

            【最近对话】
            %s
            """, summary, recentContext);

        log.info("[上下文压缩] 完成，摘要长度: {} 字符", result.length());
        return result;
    }

    /**
     * 生成对话摘要
     */
    private String generateSummary(List<ChatMessage> messages) {
        try {
            String conversationText = messages.stream()
                    .map(msg -> String.format("%s: %s",
                            msg.getRole() == ChatMessage.MessageRole.USER ? "用户" : "助手",
                            truncateText(msg.getContent(), 100)))
                    .collect(Collectors.joining("\n"));

            String promptTemplate = """
                请对以下对话进行简洁摘要，提取关键信息点，限制在100字以内：

                {conversation}

                摘要：
                """;

            SystemPromptTemplate template = new SystemPromptTemplate(promptTemplate);
            Prompt prompt = template.create(Map.of("conversation", conversationText));

            String summary = chatModel.call(prompt.getContents());

            // 截断过长的摘要
            if (summary.length() > SUMMARY_MAX_LENGTH) {
                summary = summary.substring(0, SUMMARY_MAX_LENGTH) + "...";
            }

            return summary;
        } catch (Exception e) {
            log.warn("[上下文压缩] 生成摘要失败: {}, 使用简单拼接", e.getMessage());
            // 降级方案：简单拼接早期消息
            return messages.stream()
                    .limit(3)
                    .map(msg -> truncateText(msg.getContent(), 50))
                    .collect(Collectors.joining("; "));
        }
    }

    /**
     * 格式化消息列表为字符串
     */
    private String formatMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        return messages.stream()
                .map(msg -> String.format("%s: %s",
                        msg.getRole() == ChatMessage.MessageRole.USER ? "用户" : "助手",
                        msg.getContent()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 截断文本
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 估算Token数量（简化版：中文字符按1个Token，英文按0.25个Token）
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;

        int chineseCount = (int) text.chars()
                .filter(c -> c >= 0x4E00 && c <= 0x9FA5)
                .count();
        int otherCount = text.length() - chineseCount;

        return chineseCount + (int) (otherCount * 0.25);
    }

    /**
     * 检查是否需要压缩
     */
    public boolean needCompression(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        // 排除当前用户消息，只计算历史消息数量
        int historyCount = messages.size() > 1 ? messages.size() - 1 : 0;
        return historyCount > SUMMARIZE_THRESHOLD;
    }
}

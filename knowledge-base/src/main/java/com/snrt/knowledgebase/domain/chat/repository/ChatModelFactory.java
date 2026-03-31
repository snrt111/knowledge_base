package com.snrt.knowledgebase.domain.chat.repository;

import com.snrt.knowledgebase.common.constants.Constants;
import com.snrt.knowledgebase.common.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelFactory {

    private final List<ChatModelProvider> providerList;
    private final Map<String, ChatModelProvider> providers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        providerList.forEach(provider -> {
            providers.put(provider.getProviderName(), provider);
            log.info("注册ChatModelProvider: {}", provider.getProviderName());
        });
    }

    public ChatModel getModel(String provider) {
        return Optional.ofNullable(providers.get(provider))
                .map(ChatModelProvider::createModel)
                .orElseThrow(() -> new ValidationException("provider", "不支持的模型提供商: " + provider));
    }

    public ChatModel getDefaultModel() {
        return getModel(Constants.Chat.DEFAULT_PROVIDER);
    }

    public boolean isSupported(String provider) {
        return providers.containsKey(provider);
    }
}

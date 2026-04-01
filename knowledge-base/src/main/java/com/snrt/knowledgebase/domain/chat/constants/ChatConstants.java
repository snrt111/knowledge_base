package com.snrt.knowledgebase.domain.chat.constants;

public class ChatConstants {

    private ChatConstants() {
    }

    public static class Session {
        public static final int MAX_TITLE_LENGTH = 100;
        public static final int DEFAULT_MESSAGE_PREVIEW_LENGTH = 20;
        public static final String DEFAULT_TITLE = "新对话";
    }

    public static class Message {
        public static final int MAX_CONTENT_LENGTH = 2000;
        public static final String ROLE_USER = "user";
        public static final String ROLE_ASSISTANT = "assistant";
        public static final String ROLE_SYSTEM = "system";
    }

    public static class Retrieval {
        public static final int DEFAULT_TOP_K = 20;
        public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;
        public static final int MAX_RETRIEVAL_RESULTS = 10;
        public static final double MIN_SCORE_THRESHOLD = 0.5;
    }

    public static class Cache {
        public static final String PREFIX_SEARCH_RESULT = "rag:search:";
        public static final long DEFAULT_TTL_SECONDS = 3600;
    }
}

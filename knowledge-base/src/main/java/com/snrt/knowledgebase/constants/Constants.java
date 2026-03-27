package com.snrt.knowledgebase.constants;

import java.util.Set;

public final class Constants {

    private Constants() {
    }

    public static final class File {
        public static final long MAX_SIZE = 50 * 1024 * 1024;
        // 只支持 PDF、Word、Excel、PPT、Markdown、TXT
        public static final Set<String> ALLOWED_TYPES = Set.of(
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "txt"
        );
        public static final Set<String> TEXT_TYPES = Set.of("txt", "md");
        public static final Set<String> PDF_TYPES = Set.of("pdf");
        public static final Set<String> WORD_TYPES = Set.of("doc", "docx");
        public static final Set<String> EXCEL_TYPES = Set.of("xls", "xlsx");
        public static final Set<String> PPT_TYPES = Set.of("ppt", "pptx");
        public static final String UPLOAD_DIR = "uploads/documents/";
        public static final String MINIO_DOCUMENT_PREFIX = "documents/";
        public static final int DEFAULT_EXPIRY_HOURS = 24;
    }

    public static final class Pagination {
        public static final int DEFAULT_PAGE = 1;
        public static final int DEFAULT_SIZE = 10;
        public static final int MAX_SIZE = 100;
    }

    public static final class Cache {
        public static final String KNOWLEDGE_BASE = "knowledgeBase";
        public static final String DOCUMENT = "document";
        public static final String KNOWLEDGE_BASE_LIST = "knowledgeBaseList";
        public static final long DEFAULT_TTL_MINUTES = 10;
    }

    public static final class Chat {
        public static final int MAX_CONTEXT_LENGTH = 10;
        public static final int MAX_MESSAGE_LENGTH = 5000;
        public static final String DEFAULT_PROVIDER = "zhipuai";
        public static final String ZHIPU_AI_PROVIDER = "zhipuai";
        public static final String OLLAMA_PROVIDER = "ollama";
    }

    public static final class DocumentStatus {
        public static final String PROCESSING = "processing";
        public static final String COMPLETED = "completed";
        public static final String FAILED = "failed";
    }

    public static final class VectorStore {
        // 传统分块配置（已弃用，保留兼容）
        public static final int CHUNK_SIZE = 1000;
        public static final int CHUNK_OVERLAP = 200;
        
        // 智能语义分块配置
        public static final int SEMANTIC_CHUNK_TARGET_SIZE = 800;   // 目标块大小
        public static final int SEMANTIC_CHUNK_MIN_SIZE = 300;      // 最小块大小
        public static final int SEMANTIC_CHUNK_MAX_SIZE = 1500;     // 最大块大小
        public static final int SEMANTIC_CHUNK_OVERLAP = 100;       // 重叠大小
        
        // 元数据字段
        public static final String METADATA_DOCUMENT_ID = "documentId";
        public static final String METADATA_DOCUMENT_NAME = "documentName";
        public static final String METADATA_KNOWLEDGE_BASE_ID = "knowledgeBaseId";
        public static final String METADATA_KNOWLEDGE_BASE_NAME = "knowledgeBaseName";
        public static final String METADATA_CHUNK_ID = "chunkId";
        public static final String METADATA_FILE_PATH = "filePath";
        
        // 智能分块新增元数据
        public static final String METADATA_CHUNK_INDEX = "chunkIndex";
        public static final String METADATA_CHUNK_TOTAL = "chunkTotal";
        public static final String METADATA_HEADING_CONTEXT = "headingContext";
        public static final String METADATA_SEMANTIC_UNITS = "semanticUnits";
        public static final String METADATA_HIERARCHY_LEVEL = "hierarchyLevel";
    }

    public static final class Async {
        public static final String DOCUMENT_PROCESSOR_EXECUTOR = "documentProcessorExecutor";
        public static final String CHAT_STREAM_EXECUTOR = "chatStreamExecutor";
    }
}

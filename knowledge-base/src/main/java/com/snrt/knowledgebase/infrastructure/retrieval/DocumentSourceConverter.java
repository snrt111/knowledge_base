package com.snrt.knowledgebase.infrastructure.retrieval;

import com.snrt.knowledgebase.config.RetrievalConfig;
import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentSourceConverter {

    private static final int DEFAULT_MAX_SNIPPET_COUNT = 3;

    public static List<DocumentSourceDTO> convert(List<Document> documents, RetrievalConfig config) {
        return documents.stream()
                .collect(Collectors.groupingBy(DocumentSourceConverter::getDocumentId))
                .entrySet().stream()
                .map(entry -> createDocumentSource(entry.getKey(), entry.getValue(), config))
                .sorted((s1, s2) -> {
                    Double score1 = s1.getScore();
                    Double score2 = s2.getScore();
                    if (score1 == null && score2 == null) return 0;
                    if (score1 == null) return 1;
                    if (score2 == null) return -1;
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());
    }

    private static DocumentSourceDTO createDocumentSource(String docId, List<Document> chunks, RetrievalConfig config) {
        Document firstChunk = chunks.get(0);

        String docName = getMetadataString(firstChunk, "document_name", "未知文档");
        String kbName = getMetadataString(firstChunk, "knowledge_base_name", "未知知识库");

        double score = calculateScore(chunks, config);

        int maxSnippetCount = config.getVector().getDefaultTopK();
        if (maxSnippetCount <= 0) {
            maxSnippetCount = DEFAULT_MAX_SNIPPET_COUNT;
        }

        List<String> snippets = chunks.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isEmpty())
                .limit(maxSnippetCount)
                .collect(Collectors.toList());

        return DocumentSourceDTO.builder()
                .documentId("unknown".equals(docId) ? null : docId)
                .documentName(docName)
                .knowledgeBaseName(kbName)
                .score(score)
                .snippet(snippets.isEmpty() ? "" : snippets.get(0))
                .snippets(snippets)
                .build();
    }

    private static double calculateScore(List<Document> chunks, RetrievalConfig config) {
        double rrfWeight = 0.3;
        double ruleWeight = 0.5;
        double kgWeight = 0.2;

        return chunks.stream()
                .mapToDouble(doc -> {
                    Double rrf = getMetadataDouble(doc, "rrf_score");
                    Double rule = getMetadataDouble(doc, "rule_score");
                    Double kg = getMetadataDouble(doc, "kg_score");
                    
                    double score = 0;
                    if (rrf != null) score += rrf * rrfWeight;
                    if (rule != null) score += rule * ruleWeight;
                    if (kg != null) score += kg * kgWeight;
                    
                    return score;
                })
                .max()
                .orElse(0.0);
    }

    private static Double getMetadataDouble(Document doc, String key) {
        Object value = doc.getMetadata().get(key);
        return value instanceof Double ? (Double) value : 0.0;
    }

    private static String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get("document_id");
        return docId != null ? docId.toString() : "unknown";
    }
}

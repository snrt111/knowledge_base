package com.snrt.knowledgebase.domain.chat.service;

import com.snrt.knowledgebase.common.constants.Constants;
import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DocumentSourceConverter {

    private static final double MIN_SCORE_THRESHOLD = 0.5;

    public List<DocumentSourceDTO> convert(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        Map<String, List<Document>> docsById = documents.stream()
                .collect(Collectors.groupingBy(this::getDocumentId));

        return docsById.entrySet().stream()
                .map(entry -> createDocumentSource(entry.getKey(), entry.getValue()))
                .filter(source -> source.getScore() != null && source.getScore() >= MIN_SCORE_THRESHOLD)
                .sorted(this::compareByScore)
                .collect(Collectors.toList());
    }

    private DocumentSourceDTO createDocumentSource(String docId, List<Document> chunks) {
        Document firstChunk = chunks.get(0);

        String docName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_DOCUMENT_NAME, "未知文档");
        String kbName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_KNOWLEDGE_BASE_NAME, "未知知识库");
        double score = calculateMaxScore(chunks);
        List<String> snippets = extractSnippets(chunks);

        return DocumentSourceDTO.builder()
                .documentId("unknown".equals(docId) ? null : docId)
                .documentName(docName)
                .knowledgeBaseName(kbName)
                .score(score)
                .snippet(snippets.isEmpty() ? "" : snippets.get(0))
                .snippets(snippets)
                .build();
    }

    private double calculateMaxScore(List<Document> chunks) {
        return chunks.stream()
                .mapToDouble(this::calculateDocumentScore)
                .max()
                .orElse(0.0);
    }

    private double calculateDocumentScore(Document doc) {
        Double rrf = (Double) doc.getMetadata().get("rrf_score");
        Double rule = (Double) doc.getMetadata().get("rule_score");
        double score = 0;
        if (rrf != null) score += rrf * 0.3;
        if (rule != null) score += rule * 0.7;
        return score;
    }

    private List<String> extractSnippets(List<Document> chunks) {
        return chunks.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isEmpty())
                .limit(3)
                .collect(Collectors.toList());
    }

    private String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_ID);
        return docId != null ? docId.toString() : "unknown";
    }

    private String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int compareByScore(DocumentSourceDTO s1, DocumentSourceDTO s2) {
        Double score1 = s1.getScore();
        Double score2 = s2.getScore();
        if (score1 == null && score2 == null) return 0;
        if (score1 == null) return 1;
        if (score2 == null) return -1;
        return Double.compare(score2, score1);
    }
}

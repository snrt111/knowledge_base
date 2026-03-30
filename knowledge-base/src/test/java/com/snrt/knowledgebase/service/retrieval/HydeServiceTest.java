package com.snrt.knowledgebase.service.retrieval;

import com.snrt.knowledgebase.model.ChatModelFactory;
import com.snrt.knowledgebase.service.RAGCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HyDE服务测试
 *
 * @author SNRT
 * @since 1.0
 */
@SpringBootTest
class HydeServiceTest {

    @Autowired
    private HydeService hydeService;

    @Test
    void testGenerateHypotheticalAnswer() {
        String query = "什么是人工智能？";
        String hypotheticalAnswer = hydeService.generateHypotheticalAnswer(query);
        
        System.out.println("Query: " + query);
        System.out.println("Hypothetical Answer: " + hypotheticalAnswer);
        
        assertNotNull(hypotheticalAnswer);
        assertFalse(hypotheticalAnswer.isEmpty());
        assertTrue(hypotheticalAnswer.length() > 50); // 确保生成的答案有一定长度
    }

    @Test
    void testShouldUseHyde() {
        // 短查询不使用HyDE
        String shortQuery = "你好";
        assertFalse(hydeService.shouldUseHyde(shortQuery));
        
        // 长查询使用HyDE
        String longQuery = "人工智能在医疗领域的应用有哪些具体案例？";
        assertTrue(hydeService.shouldUseHyde(longQuery));
    }

    @Test
    void testRetrieveWithHyde() {
        // 注意：这个测试需要实际的知识库和文档
        // 这里只是验证方法能正常调用
        String query = "什么是Spring Boot？";
        String knowledgeBaseId = "test-kb";
        int topK = 5;
        
        List<Document> results = hydeService.retrieveWithHyde(query, knowledgeBaseId, topK);
        
        // 即使没有找到文档，方法也应该正常执行
        assertNotNull(results);
        System.out.println("HyDE retrieval results: " + results.size() + " documents");
    }
}

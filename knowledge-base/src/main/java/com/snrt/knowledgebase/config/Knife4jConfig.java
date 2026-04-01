package com.snrt.knowledgebase.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置
 * 
 * 配置Swagger/OpenAPI文档：
 * - API基本信息
 * - 联系方式
 * - 分组配置
 * 
 * @author SNRT
 * @since 1.0
 */
@Configuration
public class Knife4jConfig {

    /**
     * 创建自定义OpenAPI文档
     * 
     * @return OpenAPI实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("知识库管理系统 API 文档")
                        .version("v1.0.0")
                        .description("基于 Spring AI 和 RAG 技术的智能知识库管理系统，支持文档管理、向量检索、智能问答等功能")
                        .contact(new Contact()
                                .name("snrt111")
                                .email("snrt111@163.com")
                                .url("https://github.com/snrt111/knowledge-base"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    /**
     * 创建默认API分组
     * 
     * @return GroupedOpenApi实例
     */
    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .displayName("知识库管理系统 API")
                .packagesToScan("com.snrt.knowledgebase.controller")
                .build();
    }
}

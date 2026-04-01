package com.snrt.knowledgebase.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * MinIO配置
 * 
 * 配置MinIO对象存储服务的连接参数
 * 提供MinioClient Bean用于文件上传、下载等操作
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    /**
     * 创建MinIO客户端Bean
     * 
     * @return MinioClient实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}

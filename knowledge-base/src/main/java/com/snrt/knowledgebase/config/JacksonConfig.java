package com.snrt.knowledgebase.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson序列化配置
 * 
 * 配置Java 8日期时间类型的序列化支持：
 * - LocalDateTime的序列化和反序列化
 * - 统一日期格式：yyyy-MM-dd HH:mm:ss
 * 
 * @author SNRT
 * @since 1.0
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * 配置全局ObjectMapper
     * 
     * 注册JavaTime模块以支持Java 8日期时间类型
     * 
     * @return ObjectMapper实例
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册JavaTime模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // 配置LocalDateTime的序列化格式
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        
        objectMapper.registerModule(javaTimeModule);
        
        return objectMapper;
    }

    /**
     * 配置RabbitMQ的MessageConverter
     * 
     * 使用自定义的ObjectMapper来处理消息转换
     * 
     * @param objectMapper ObjectMapper实例
     * @return MessageConverter实例
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        // 设置默认内容类型为 application/json
        converter.setCreateMessageIds(true);
        return converter;
    }
}

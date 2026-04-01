package com.snrt.knowledgebase.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 * 
 * 配置RabbitMQ消息队列：
 * - 文档处理队列和交换机
 * - 死信队列和死信交换机（用于重试）
 * - JSON消息转换器
 * - RabbitTemplate和确认回调
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String DOCUMENT_PROCESS_QUEUE = "document.process.queue";
    public static final String DOCUMENT_PROCESS_EXCHANGE = "document.process.exchange";
    public static final String DOCUMENT_PROCESS_ROUTING_KEY = "document.process";

    public static final String DOCUMENT_PROCESS_DLQ = "document.process.dlq";
    public static final String DOCUMENT_PROCESS_DLX = "document.process.dlx";
    public static final String DOCUMENT_PROCESS_DLQ_ROUTING_KEY = "document.process.dlq";

    @Value("${rabbitmq.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${rabbitmq.retry.initial-interval:5000}")
    private long initialRetryInterval;

    /**
     * 创建文档处理队列
     * 
     * 配置队列属性：
     * - 持久化队列
     * - 死信交换机（DLX）用于消息重试
     * - 消息过期时间（TTL）5分钟
     * 
     * @return 文档处理队列
     */
    @Bean
    public Queue documentProcessQueue() {
        return QueueBuilder.durable(DOCUMENT_PROCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DOCUMENT_PROCESS_DLX)
                .withArgument("x-dead-letter-routing-key", DOCUMENT_PROCESS_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    /**
     * 创建文档处理交换机
     * 
     * @return 文档处理交换机
     */
    @Bean
    public DirectExchange documentProcessExchange() {
        return new DirectExchange(DOCUMENT_PROCESS_EXCHANGE);
    }

    /**
     * 创建文档处理队列绑定
     * 
     * 将文档处理队列绑定到文档处理交换机
     * 
     * @return 文档处理队列绑定
     */
    @Bean
    public Binding documentProcessBinding() {
        return BindingBuilder
                .bind(documentProcessQueue())
                .to(documentProcessExchange())
                .with(DOCUMENT_PROCESS_ROUTING_KEY);
    }

    /**
     * 创建死信队列
     * 
     * 用于存储处理失败的消息，支持重试机制
     * 
     * @return 死信队列
     */
    @Bean
    public Queue documentProcessDLQ() {
        return QueueBuilder.durable(DOCUMENT_PROCESS_DLQ).build();
    }

    /**
     * 创建死信交换机
     * 
     * @return 死信交换机
     */
    @Bean
    public DirectExchange documentProcessDLX() {
        return new DirectExchange(DOCUMENT_PROCESS_DLX);
    }

    /**
     * 创建死信队列绑定
     * 
     * 将死信队列绑定到死信交换机
     * 
     * @return 死信队列绑定
     */
    @Bean
    public Binding documentProcessDLQBinding() {
        return BindingBuilder
                .bind(documentProcessDLQ())
                .to(documentProcessDLX())
                .with(DOCUMENT_PROCESS_DLQ_ROUTING_KEY);
    }

    /**
     * 创建JSON消息转换器
     * 
     * 使用Jackson将消息对象转换为JSON格式
     * 
     * @param objectMapper ObjectMapper实例
     * @return JSON消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 创建RabbitTemplate
     * 
     * 配置消息发送确认回调：
     * - 成功发送：记录成功日志
     * - 发送失败：记录错误日志
     * 
     * @param connectionFactory 连接工厂
     * @param messageConverter 消息转换器
     * @return RabbitTemplate实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息发送失败：{}", cause);
            }
        });
        return template;
    }
}

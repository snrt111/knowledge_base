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

    @Bean
    public Queue documentProcessQueue() {
        return QueueBuilder.durable(DOCUMENT_PROCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DOCUMENT_PROCESS_DLX)
                .withArgument("x-dead-letter-routing-key", DOCUMENT_PROCESS_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public DirectExchange documentProcessExchange() {
        return new DirectExchange(DOCUMENT_PROCESS_EXCHANGE);
    }

    @Bean
    public Binding documentProcessBinding() {
        return BindingBuilder
                .bind(documentProcessQueue())
                .to(documentProcessExchange())
                .with(DOCUMENT_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Queue documentProcessDLQ() {
        return QueueBuilder.durable(DOCUMENT_PROCESS_DLQ).build();
    }

    @Bean
    public DirectExchange documentProcessDLX() {
        return new DirectExchange(DOCUMENT_PROCESS_DLX);
    }

    @Bean
    public Binding documentProcessDLQBinding() {
        return BindingBuilder
                .bind(documentProcessDLQ())
                .to(documentProcessDLX())
                .with(DOCUMENT_PROCESS_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

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

package com.example.resourceapi.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.rabbitmq")
public record RabbitProperties (String exchangeName, String queueName, String routingKey) {}

package com.example.resourceapi.rabbit.publisher;

import com.example.resourceapi.config.props.RabbitProperties;
import com.example.resourceapi.rabbit.event.BookLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookLogEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties rabbitProperties;

    public void publishEvent(BookLogEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    rabbitProperties.exchangeName(),
                    rabbitProperties.routingKey(),
                    event
            );
            log.info("Sent event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event, e);
        }
    }
}

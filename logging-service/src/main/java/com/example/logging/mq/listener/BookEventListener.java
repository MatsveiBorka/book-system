package com.example.logging.mq.listener;

import com.example.logging.mq.event.BookLogEvent;
import com.example.logging.mq.handler.BookEventHandler;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BookEventListener {

    private final BookEventHandler bookEventHandler;

    @RabbitListener(queues = "${spring.rabbitmq.queue-name}")
    public void handleMessage(BookLogEvent event) {
        bookEventHandler.processEvent(event);
    }
}

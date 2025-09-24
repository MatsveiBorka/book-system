package com.example.logging.mq.handler;

import com.example.logging.entity.EventLog;
import com.example.logging.enums.EventType;
import com.example.logging.mq.event.BookLogEvent;
import com.example.logging.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
@Slf4j
public class BookEventHandler {

    private final EventLogRepository eventLogRepository;

    public void processEvent(BookLogEvent event) {
        log.info("Received event: {}", event);
        EventLog logEntity = new EventLog();
        String eventType = Optional.of(event).map(BookLogEvent::getEventType)
                .map(EventType::name)
                .orElseThrow(() -> new RuntimeException("BookLogEvent is not valid"));
        logEntity.setEventType(eventType);
        logEntity.setTimestamp(event.getTimestamp());
        logEntity.setSubjectType(event.getSubjectType());
        logEntity.setDescription(event.getEventDescription());

        eventLogRepository.save(logEntity);
    }
}

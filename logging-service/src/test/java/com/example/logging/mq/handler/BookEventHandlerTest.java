package com.example.logging.mq.handler;

import com.example.logging.entity.EventLog;
import com.example.logging.enums.EventType;
import com.example.logging.mq.event.BookLogEvent;
import com.example.logging.repository.EventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookEventHandler Unit Tests")
class BookEventHandlerTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @InjectMocks
    private BookEventHandler bookEventHandler;

    private BookLogEvent testBookLogEvent;

    @BeforeEach
    void setUp() {
        testBookLogEvent = new BookLogEvent();
        testBookLogEvent.setEventType(EventType.CREATE);
        testBookLogEvent.setTimestamp(Instant.now());
        testBookLogEvent.setSubjectType("BOOK");
        testBookLogEvent.setEventDescription("New book created: Spring Boot Guide");
    }

    @Test
    @DisplayName("Should process book event and save to repository successfully")
    void shouldProcessBookEventAndSaveToRepositorySuccessfully() {
        // Given
        ArgumentCaptor<EventLog> eventLogCaptor = ArgumentCaptor.forClass(EventLog.class);

        // When
        bookEventHandler.processEvent(testBookLogEvent);

        // Then
        verify(eventLogRepository).save(eventLogCaptor.capture());

        EventLog savedEventLog = eventLogCaptor.getValue();
        assertNotNull(savedEventLog);
        assertEquals("CREATE", savedEventLog.getEventType());
        assertEquals(testBookLogEvent.getTimestamp(), savedEventLog.getTimestamp());
        assertEquals("BOOK", savedEventLog.getSubjectType());
        assertEquals("New book created: Spring Boot Guide", savedEventLog.getDescription());
    }

    @Test
    @DisplayName("Should process UPDATE event correctly")
    void shouldProcessUpdateEventCorrectly() {
        // Given
        testBookLogEvent.setEventType(EventType.UPDATE);
        testBookLogEvent.setEventDescription("Book updated: Spring Boot Advanced");
        ArgumentCaptor<EventLog> eventLogCaptor = ArgumentCaptor.forClass(EventLog.class);

        // When
        bookEventHandler.processEvent(testBookLogEvent);

        // Then
        verify(eventLogRepository).save(eventLogCaptor.capture());

        EventLog savedEventLog = eventLogCaptor.getValue();
        assertEquals("UPDATE", savedEventLog.getEventType());
        assertEquals("Book updated: Spring Boot Advanced", savedEventLog.getDescription());
    }

    @Test
    @DisplayName("Should process DELETE event correctly")
    void shouldProcessDeleteEventCorrectly() {
        // Given
        testBookLogEvent.setEventType(EventType.DELETE);
        testBookLogEvent.setEventDescription("Book deleted with ID: 123");
        ArgumentCaptor<EventLog> eventLogCaptor = ArgumentCaptor.forClass(EventLog.class);

        // When
        bookEventHandler.processEvent(testBookLogEvent);

        // Then
        verify(eventLogRepository).save(eventLogCaptor.capture());

        EventLog savedEventLog = eventLogCaptor.getValue();
        assertEquals("DELETE", savedEventLog.getEventType());
        assertEquals("Book deleted with ID: 123", savedEventLog.getDescription());
    }

    @Test
    @DisplayName("Should throw exception when event type is null")
    void shouldThrowExceptionWhenEventTypeIsNull() {
        // Given
        testBookLogEvent.setEventType(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> bookEventHandler.processEvent(testBookLogEvent));

        assertEquals("BookLogEvent is not valid", exception.getMessage());
        verify(eventLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle event with null description")
    void shouldHandleEventWithNullDescription() {
        // Given
        testBookLogEvent.setEventDescription(null);
        ArgumentCaptor<EventLog> eventLogCaptor = ArgumentCaptor.forClass(EventLog.class);

        // When
        bookEventHandler.processEvent(testBookLogEvent);

        // Then
        verify(eventLogRepository).save(eventLogCaptor.capture());

        EventLog savedEventLog = eventLogCaptor.getValue();
        assertNull(savedEventLog.getDescription());
        assertEquals("CREATE", savedEventLog.getEventType());
    }
}

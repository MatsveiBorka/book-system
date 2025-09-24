package com.example.resourceapi.rabbit.publisher;

import com.example.resourceapi.config.props.RabbitProperties;
import com.example.resourceapi.enums.EventType;
import com.example.resourceapi.rabbit.event.BookLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookLogEventPublisher Unit Tests")
class BookLogEventPublisherUnitTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitProperties rabbitProperties;

    @InjectMocks
    private BookLogEventPublisher bookLogEventPublisher;

    private BookLogEvent testEvent;
    private static final String TEST_EXCHANGE_NAME = "test.book.exchange";
    private static final String TEST_ROUTING_KEY = "test.book.events";

    @BeforeEach
    void setUp() {
        testEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.CREATE,
                "Unit test book was created"
        );

        lenient().when(rabbitProperties.exchangeName()).thenReturn(TEST_EXCHANGE_NAME);
        lenient().when(rabbitProperties.routingKey()).thenReturn(TEST_ROUTING_KEY);
    }

    @Test
    @DisplayName("Should publish event with correct parameters")
    void shouldPublishEventWithCorrectParameters() {
        // When
        bookLogEventPublisher.publishEvent(testEvent);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(TEST_EXCHANGE_NAME),
                eq(TEST_ROUTING_KEY),
                eq(testEvent)
        );
        verify(rabbitProperties).exchangeName();
        verify(rabbitProperties).routingKey();
    }

    @Test
    @DisplayName("Should handle null event gracefully")
    void shouldHandleNullEventGracefully() {
        // When
        bookLogEventPublisher.publishEvent(null);

        // Then - The publisher should still call rabbitProperties and attempt to publish with null event
        verify(rabbitTemplate).convertAndSend(
                eq(TEST_EXCHANGE_NAME),
                eq(TEST_ROUTING_KEY),
                eq((BookLogEvent) null)
        );
        verify(rabbitProperties).exchangeName();
        verify(rabbitProperties).routingKey();
    }

    @Test
    @DisplayName("Should capture event details correctly")
    void shouldCaptureEventDetailsCorrectly() {
        // When
        bookLogEventPublisher.publishEvent(testEvent);

        // Then
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BookLogEvent> eventCaptor = ArgumentCaptor.forClass(BookLogEvent.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture()
        );

        assertEquals(TEST_EXCHANGE_NAME, exchangeCaptor.getValue());
        assertEquals(TEST_ROUTING_KEY, routingKeyCaptor.getValue());
        assertEquals(testEvent, eventCaptor.getValue());
    }

    @Test
    @DisplayName("Should not interact with RabbitTemplate when not publishing")
    void shouldNotInteractWithRabbitTemplateWhenNotPublishing() {
        // Given - Reset any interactions from setup
        reset(rabbitTemplate, rabbitProperties);

        // When - Don't call publishEvent
        // Then - No interactions should occur
        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(rabbitProperties);
    }

    @Test
    @DisplayName("Should handle multiple events in sequence")
    void shouldHandleMultipleEventsInSequence() {
        // Given
        BookLogEvent event1 = new BookLogEvent(Instant.now(), "Book", EventType.CREATE, "First event");
        BookLogEvent event2 = new BookLogEvent(Instant.now(), "Book", EventType.UPDATE, "Second event");
        BookLogEvent event3 = new BookLogEvent(Instant.now(), "Book", EventType.DELETE, "Third event");

        // When
        bookLogEventPublisher.publishEvent(event1);
        bookLogEventPublisher.publishEvent(event2);
        bookLogEventPublisher.publishEvent(event3);

        // Then
        verify(rabbitTemplate, times(3)).convertAndSend(
                eq(TEST_EXCHANGE_NAME),
                eq(TEST_ROUTING_KEY),
                any(BookLogEvent.class)
        );
        verify(rabbitProperties, times(3)).exchangeName();
        verify(rabbitProperties, times(3)).routingKey();
    }

    @Test
    @DisplayName("Should handle different event types")
    void shouldHandleDifferentEventTypes() {
        // Given
        BookLogEvent createEvent = new BookLogEvent(Instant.now(), "Book", EventType.CREATE, "Create event");
        BookLogEvent updateEvent = new BookLogEvent(Instant.now(), "Book", EventType.UPDATE, "Update event");
        BookLogEvent deleteEvent = new BookLogEvent(Instant.now(), "Book", EventType.DELETE, "Delete event");

        // When & Then - CREATE
        bookLogEventPublisher.publishEvent(createEvent);
        verify(rabbitTemplate).convertAndSend(TEST_EXCHANGE_NAME, TEST_ROUTING_KEY, createEvent);

        // When & Then - UPDATE
        bookLogEventPublisher.publishEvent(updateEvent);
        verify(rabbitTemplate).convertAndSend(TEST_EXCHANGE_NAME, TEST_ROUTING_KEY, updateEvent);

        // When & Then - DELETE
        bookLogEventPublisher.publishEvent(deleteEvent);
        verify(rabbitTemplate).convertAndSend(TEST_EXCHANGE_NAME, TEST_ROUTING_KEY, deleteEvent);

        verify(rabbitTemplate, times(3)).convertAndSend(anyString(), anyString(), any(BookLogEvent.class));
    }

    @Test
    @DisplayName("Should use correct properties from configuration")
    void shouldUseCorrectPropertiesFromConfiguration() {
        // Given
        String customExchange = "custom.exchange";
        String customRoutingKey = "custom.routing.key";

        when(rabbitProperties.exchangeName()).thenReturn(customExchange);
        when(rabbitProperties.routingKey()).thenReturn(customRoutingKey);

        // When
        bookLogEventPublisher.publishEvent(testEvent);

        // Then
        verify(rabbitTemplate).convertAndSend(customExchange, customRoutingKey, testEvent);
        verify(rabbitProperties).exchangeName();
        verify(rabbitProperties).routingKey();
    }
}

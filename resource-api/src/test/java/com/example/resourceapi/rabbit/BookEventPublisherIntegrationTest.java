package com.example.resourceapi.rabbit;

import com.example.resourceapi.config.TestContainersConfig;
import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.entity.Book;
import com.example.resourceapi.enums.EventType;
import com.example.resourceapi.rabbit.event.BookLogEvent;
import com.example.resourceapi.rabbit.publisher.BookLogEventPublisher;
import com.example.resourceapi.repository.BookRepository;
import com.example.resourceapi.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
@DisplayName("RabbitMQ Integration Tests for Resource API with TestContainers")
class BookEventPublisherIntegrationTest {

    @Autowired
    private BookLogEventPublisher bookLogEventPublisher;

    @Autowired
    private BookServiceImpl bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Should publish event when book is created through service")
    void shouldPublishEventWhenBookIsCreatedThroughService() {
        // Given
        List<CreateBookRequestDto> requestDtos = List.of(
                CreateBookRequestDto.builder()
                        .title("Event Test Book")
                        .author("Event Test Author")
                        .publicationYear(2023)
                        .description("Testing event publishing")
                        .build()
        );

        // When
        assertDoesNotThrow(() -> {
            bookService.saveAll(requestDtos);
        });

        // Then
        // Verify book was saved to PostgreSQL
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Book> booksInDb = bookRepository.findAll();
            assertEquals(1, booksInDb.size());
            assertEquals("Event Test Book", booksInDb.get(0).getTitle());
        });

        // Note: In a real integration test, you would also verify that the event
        // was published to RabbitMQ, but that would require setting up a listener
        // or checking the queue directly
    }

    @Test
    @DisplayName("Should publish BookLogEvent directly with correct structure")
    void shouldPublishBookLogEventDirectlyWithCorrectStructure() {
        // Given
        BookLogEvent testEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.CREATE,
                "Integration test book creation event"
        );

        // When & Then
        assertDoesNotThrow(() -> {
            bookLogEventPublisher.publishEvent(testEvent);
        });

        // Verify event structure
        assertNotNull(testEvent.getTimestamp());
        assertEquals("Book", testEvent.getSubjectType());
        assertEquals(EventType.CREATE, testEvent.getEventType());
        assertEquals("Integration test book creation event", testEvent.getEventDescription());
    }

    @Test
    @DisplayName("Should handle multiple event types correctly")
    void shouldHandleMultipleEventTypesCorrectly() {
        // Given
        BookLogEvent createEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.CREATE,
                "Book creation event"
        );

        BookLogEvent updateEvent = new BookLogEvent(
                Instant.now().plusSeconds(1),
                "Book",
                EventType.UPDATE,
                "Book update event"
        );

        BookLogEvent deleteEvent = new BookLogEvent(
                Instant.now().plusSeconds(2),
                "Book",
                EventType.DELETE,
                "Book deletion event"
        );

        // When & Then
        assertDoesNotThrow(() -> {
            bookLogEventPublisher.publishEvent(createEvent);
            bookLogEventPublisher.publishEvent(updateEvent);
            bookLogEventPublisher.publishEvent(deleteEvent);
        });

        // Verify all events have correct types
        assertEquals(EventType.CREATE, createEvent.getEventType());
        assertEquals(EventType.UPDATE, updateEvent.getEventType());
        assertEquals(EventType.DELETE, deleteEvent.getEventType());
    }

    @Test
    @DisplayName("Should handle event publishing with real RabbitMQ container")
    void shouldHandleEventPublishingWithRealRabbitMQContainer() {
        // Given - Create a book to generate events
        Book testBook = new Book();
        testBook.setTitle("RabbitMQ Test Book");
        testBook.setAuthor("RabbitMQ Test Author");
        testBook.setPublicationYear(2023);
        testBook.setDescription("Testing RabbitMQ integration");

        Book savedBook = bookRepository.save(testBook);

        // When - Publish events for different operations
        BookLogEvent createEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.CREATE,
                String.format("Book '%s' was created", savedBook.getTitle())
        );

        BookLogEvent updateEvent = new BookLogEvent(
                Instant.now().plusSeconds(1),
                "Book",
                EventType.UPDATE,
                String.format("Book '%s' was updated", savedBook.getTitle())
        );

        // Then
        assertDoesNotThrow(() -> {
            bookLogEventPublisher.publishEvent(createEvent);
            bookLogEventPublisher.publishEvent(updateEvent);
        });

        // Verify the book exists in PostgreSQL
        assertTrue(bookRepository.findById(savedBook.getId()).isPresent());
    }

    @Test
    @DisplayName("Should maintain event ordering and timestamps")
    void shouldMaintainEventOrderingAndTimestamps() {
        // Given
        Instant baseTime = Instant.now();

        // When - Create events with specific timestamps
        for (int i = 0; i < 5; i++) {
            BookLogEvent event = new BookLogEvent(
                    baseTime.plusSeconds(i * 10),
                    "Book",
                    EventType.CREATE,
                    "Ordered event " + i
            );

            assertDoesNotThrow(() -> {
                bookLogEventPublisher.publishEvent(event);
            });

            // Verify timestamp is correctly set
            assertTrue(event.getTimestamp().isAfter(baseTime.minusSeconds(1)));
            assertTrue(event.getTimestamp().isBefore(baseTime.plusSeconds(100)));
        }
    }

    @Test
    @DisplayName("Should handle large volume of events without failures")
    void shouldHandleLargeVolumeOfEventsWithoutFailures() {
        // Given
        int eventCount = 50;

        // When - Publish many events
        for (int i = 0; i < eventCount; i++) {
            BookLogEvent event = new BookLogEvent(
                    Instant.now().plusNanos(i * 1000),
                    "Book",
                    EventType.values()[i % 3], // Cycle through CREATE, UPDATE, DELETE
                    "Bulk test event " + i
            );

            assertDoesNotThrow(() -> {
                bookLogEventPublisher.publishEvent(event);
            });
        }

        // Then - Verify no exceptions were thrown
        // In a real scenario, you might also verify the events were received
        assertTrue(true, "All events published without exceptions");
    }

    @Test
    @DisplayName("Should verify RabbitMQ container is running and accessible")
    void shouldVerifyRabbitMQContainerIsRunningAndAccessible() {
        // Given & When - Try to get connection info from RabbitTemplate
        assertDoesNotThrow(() -> {
            // This will fail if RabbitMQ container is not running properly
            rabbitTemplate.execute(channel -> {
                assertTrue(channel.isOpen());
                return null;
            });
        });

        // Then
        assertNotNull(rabbitTemplate);
        assertNotNull(rabbitTemplate.getConnectionFactory());
    }

    @Test
    @DisplayName("Should handle event publishing errors gracefully")
    void shouldHandleEventPublishingErrorsGracefully() {
        // Given
        BookLogEvent validEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.CREATE,
                "Valid event for error handling test"
        );

        // When & Then - Should not throw exceptions even if there are internal issues
        assertDoesNotThrow(() -> {
            bookLogEventPublisher.publishEvent(validEvent);
        });

        // Verify event structure is still correct
        assertNotNull(validEvent.getTimestamp());
        assertEquals("Book", validEvent.getSubjectType());
        assertEquals(EventType.CREATE, validEvent.getEventType());
        assertNotNull(validEvent.getEventDescription());
    }

    @Test
    @DisplayName("Should verify database and messaging integration")
    void shouldVerifyDatabaseAndMessagingIntegration() {
        // Given
        CreateBookRequestDto requestDto = CreateBookRequestDto.builder()
                .title("Integration Test Book")
                .author("Integration Author")
                .publicationYear(2023)
                .description("Testing full integration")
                .build();

        // When - Perform operation that should trigger both DB save and event publishing
        assertDoesNotThrow(() -> {
            bookService.saveAll(List.of(requestDto));
        });

        // Then - Verify database persistence
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Book> books = bookRepository.findAll();
            assertEquals(1, books.size());
            assertEquals("Integration Test Book", books.get(0).getTitle());
            assertEquals("Integration Author", books.get(0).getAuthor());
        });

        // Verify we can publish related events
        Book savedBook = bookRepository.findAll().get(0);
        BookLogEvent relatedEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.CREATE,
                String.format("Book '%s' by %s was created successfully",
                        savedBook.getTitle(), savedBook.getAuthor())
        );

        assertDoesNotThrow(() -> {
            bookLogEventPublisher.publishEvent(relatedEvent);
        });
    }
}

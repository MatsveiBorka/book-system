package com.example.logging.integration;

import com.example.logging.dto.EventLogResponseDto;
import com.example.logging.entity.EventLog;
import com.example.logging.enums.EventType;
import com.example.logging.mq.event.BookLogEvent;
import com.example.logging.mq.handler.BookEventHandler;
import com.example.logging.repository.EventLogRepository;
import com.example.logging.service.EventLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Testcontainers
@DisplayName("Logging Service End-to-End Integration Tests")
class LoggingServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("logging_integration_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventLogService eventLogService;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private BookEventHandler bookEventHandler;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        eventLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle complete flow from event processing to API retrieval")
    void shouldHandleCompleteFlowFromEventProcessingToApiRetrieval() throws Exception {
        // Given - Create and process events
        BookLogEvent createEvent = new BookLogEvent();
        createEvent.setEventType(EventType.CREATE);
        createEvent.setTimestamp(Instant.now().minus(1, ChronoUnit.HOURS));
        createEvent.setSubjectType("BOOK");
        createEvent.setEventDescription("Book created: Spring Boot Guide");

        BookLogEvent updateEvent = new BookLogEvent();
        updateEvent.setEventType(EventType.UPDATE);
        updateEvent.setTimestamp(Instant.now().minus(30, ChronoUnit.MINUTES));
        updateEvent.setSubjectType("BOOK");
        updateEvent.setEventDescription("Book updated: Spring Boot Advanced Guide");

        // When - Process events through handler
        bookEventHandler.processEvent(createEvent);
        bookEventHandler.processEvent(updateEvent);

        // Then - Verify events are stored in database
        List<EventLog> storedEvents = eventLogRepository.findAll();
        assertEquals(2, storedEvents.size());

        // And - Verify service layer retrieval
        List<EventLogResponseDto> serviceResult = eventLogService.findAll();
        assertEquals(2, serviceResult.size());

        // And - Verify API endpoint returns correct data
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].subjectType").value("BOOK"))
                .andExpect(jsonPath("$[0].eventType").value("CREATE"))
                .andExpect(jsonPath("$[0].description").value("Book created: Spring Boot Guide"))
                .andExpect(jsonPath("$[1].eventType").value("UPDATE"))
                .andExpect(jsonPath("$[1].description").value("Book updated: Spring Boot Advanced Guide"));
    }

    @Test
    @DisplayName("Should handle date range filtering end-to-end")
    void shouldHandleDateRangeFilteringEndToEnd() throws Exception {
        // Given - Create events with different timestamps
        Instant now = Instant.now();

        BookLogEvent oldEvent = new BookLogEvent();
        oldEvent.setEventType(EventType.CREATE);
        oldEvent.setTimestamp(now.minus(5, ChronoUnit.DAYS));
        oldEvent.setSubjectType("BOOK");
        oldEvent.setEventDescription("Old book created");

        BookLogEvent recentEvent = new BookLogEvent();
        recentEvent.setEventType(EventType.UPDATE);
        recentEvent.setTimestamp(now.minus(1, ChronoUnit.HOURS));
        recentEvent.setSubjectType("BOOK");
        recentEvent.setEventDescription("Recent book updated");

        // Process events
        bookEventHandler.processEvent(oldEvent);
        bookEventHandler.processEvent(recentEvent);

        // When & Then - Test date range filtering via API
        String startDate = now.minus(2, ChronoUnit.DAYS).toString();
        String endDate = now.toString();

        mockMvc.perform(get("/api/v1/events/range")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("UPDATE"))
                .andExpect(jsonPath("$[0].description").value("Recent book updated"));
    }

    @Test
    @DisplayName("Should handle multiple event types correctly")
    void shouldHandleMultipleEventTypesCorrectly() throws Exception {
        // Given - Create events of all types
        Instant now = Instant.now();

        BookLogEvent[] events = {
                createBookLogEvent(EventType.CREATE, now.minus(3, ChronoUnit.HOURS), "Book created"),
                createBookLogEvent(EventType.UPDATE, now.minus(2, ChronoUnit.HOURS), "Book updated"),
                createBookLogEvent(EventType.DELETE, now.minus(1, ChronoUnit.HOURS), "Book deleted")
        };

        // When - Process all events
        for (BookLogEvent event : events) {
            bookEventHandler.processEvent(event);
        }

        // Then - Verify all events are stored and retrievable
        List<EventLogResponseDto> allEvents = eventLogService.findAll();
        assertEquals(3, allEvents.size());

        // Verify each event type is present
        assertTrue(allEvents.stream().anyMatch(e -> "CREATE".equals(e.eventType())));
        assertTrue(allEvents.stream().anyMatch(e -> "UPDATE".equals(e.eventType())));
        assertTrue(allEvents.stream().anyMatch(e -> "DELETE".equals(e.eventType())));

        // And - Verify via API
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Should handle error scenarios gracefully")
    void shouldHandleErrorScenariosGracefully() throws Exception {
        // Given - Valid event for baseline
        BookLogEvent validEvent = createBookLogEvent(EventType.CREATE, Instant.now(), "Valid event");
        bookEventHandler.processEvent(validEvent);

        // When & Then - Test invalid date format in API
        mockMvc.perform(get("/api/v1/events/range")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2025-09-23T00:00:00Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // And - Verify valid data is still accessible
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Should maintain data consistency across components")
    void shouldMaintainDataConsistencyAcrossComponents() {
        // Given
        BookLogEvent event = createBookLogEvent(EventType.CREATE, Instant.now(), "Consistency test");

        // When
        bookEventHandler.processEvent(event);

        // Then - Verify data consistency across all layers
        List<EventLog> repositoryData = eventLogRepository.findAll();
        List<EventLogResponseDto> serviceData = eventLogService.findAll();

        assertEquals(1, repositoryData.size());
        assertEquals(1, serviceData.size());

        EventLog storedEvent = repositoryData.get(0);
        EventLogResponseDto serviceEvent = serviceData.get(0);

        // Verify data consistency
        assertEquals(storedEvent.getId(), serviceEvent.id());
        assertEquals(storedEvent.getEventType(), serviceEvent.eventType());
        assertEquals(storedEvent.getSubjectType(), serviceEvent.subjectType());
        assertEquals(storedEvent.getDescription(), serviceEvent.description());
        assertEquals(storedEvent.getTimestamp(), serviceEvent.timestamp());
    }

    private BookLogEvent createBookLogEvent(EventType eventType, Instant timestamp, String description) {
        BookLogEvent event = new BookLogEvent();
        event.setEventType(eventType);
        event.setTimestamp(timestamp);
        event.setSubjectType("BOOK");
        event.setEventDescription(description);
        return event;
    }
}

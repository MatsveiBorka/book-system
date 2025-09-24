package com.example.logging.controller;

import com.example.logging.dto.EventLogResponseDto;
import com.example.logging.service.EventLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@DisplayName("EventController Unit Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventLogService eventLogService;

    private List<EventLogResponseDto> testEventLogs;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        EventLogResponseDto eventLog1 = new EventLogResponseDto(
                UUID.randomUUID(),
                now,
                "BOOK",
                "CREATE",
                "Book created successfully"
        );

        EventLogResponseDto eventLog2 = new EventLogResponseDto(
                UUID.randomUUID(),
                now.plus(1, ChronoUnit.HOURS),
                "BOOK",
                "UPDATE",
                "Book updated successfully"
        );

        testEventLogs = Arrays.asList(eventLog1, eventLog2);
    }

    @Test
    @DisplayName("Should get all events successfully")
    void shouldGetAllEventsSuccessfully() throws Exception {
        // Given
        when(eventLogService.findAll()).thenReturn(testEventLogs);

        // When & Then
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].subjectType").value("BOOK"))
                .andExpect(jsonPath("$[0].eventType").value("CREATE"))
                .andExpect(jsonPath("$[0].description").value("Book created successfully"))
                .andExpect(jsonPath("$[1].eventType").value("UPDATE"))
                .andExpect(jsonPath("$[1].description").value("Book updated successfully"));

        verify(eventLogService).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no events exist")
    void shouldReturnEmptyListWhenNoEventsExist() throws Exception {
        // Given
        when(eventLogService.findAll()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(eventLogService).findAll();
    }

    @Test
    @DisplayName("Should get events by date range successfully")
    void shouldGetEventsByDateRangeSuccessfully() throws Exception {
        // Given
        Instant startDate = Instant.parse("2025-09-20T00:00:00Z");
        Instant endDate = Instant.parse("2025-09-22T23:59:59Z");

        when(eventLogService.findEventsByDateRange(startDate, endDate)).thenReturn(testEventLogs);

        // When & Then
        mockMvc.perform(get("/api/v1/events/range")
                        .param("startDate", "2025-09-20T00:00:00Z")
                        .param("endDate", "2025-09-22T23:59:59Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].subjectType").value("BOOK"))
                .andExpect(jsonPath("$[1].subjectType").value("BOOK"));

        verify(eventLogService).findEventsByDateRange(startDate, endDate);
    }

    @Test
    @DisplayName("Should return empty list for date range with no events")
    void shouldReturnEmptyListForDateRangeWithNoEvents() throws Exception {
        // Given
        Instant startDate = Instant.parse("2025-09-20T00:00:00Z");
        Instant endDate = Instant.parse("2025-09-22T23:59:59Z");

        when(eventLogService.findEventsByDateRange(startDate, endDate)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/events/range")
                        .param("startDate", "2025-09-20T00:00:00Z")
                        .param("endDate", "2025-09-22T23:59:59Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(eventLogService).findEventsByDateRange(startDate, endDate);
    }

    @Test
    @DisplayName("Should handle invalid date format gracefully")
    void shouldHandleInvalidDateFormatGracefully() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/events/range")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2025-09-22T23:59:59Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(eventLogService, never()).findEventsByDateRange(any(), any());
    }
}

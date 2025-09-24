package com.example.logging.service;

import com.example.logging.dto.EventLogResponseDto;
import com.example.logging.entity.EventLog;
import com.example.logging.mapper.EventLogMapper;
import com.example.logging.repository.EventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventLogService Unit Tests")
class EventLogServiceTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private EventLogMapper eventLogMapper;

    @InjectMocks
    private EventLogService eventLogService;

    private EventLog testEventLog1;
    private EventLog testEventLog2;
    private EventLogResponseDto testResponseDto1;
    private EventLogResponseDto testResponseDto2;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        testEventLog1 = new EventLog();
        testEventLog1.setId(UUID.randomUUID());
        testEventLog1.setTimestamp(now);
        testEventLog1.setSubjectType("BOOK");
        testEventLog1.setEventType("CREATED");
        testEventLog1.setDescription("Book created successfully");

        testEventLog2 = new EventLog();
        testEventLog2.setId(UUID.randomUUID());
        testEventLog2.setTimestamp(now.plus(1, ChronoUnit.HOURS));
        testEventLog2.setSubjectType("BOOK");
        testEventLog2.setEventType("UPDATED");
        testEventLog2.setDescription("Book updated successfully");

        testResponseDto1 = new EventLogResponseDto(
                testEventLog1.getId(),
                testEventLog1.getTimestamp(),
                testEventLog1.getSubjectType(),
                testEventLog1.getEventType(),
                testEventLog1.getDescription()
        );

        testResponseDto2 = new EventLogResponseDto(
                testEventLog2.getId(),
                testEventLog2.getTimestamp(),
                testEventLog2.getSubjectType(),
                testEventLog2.getEventType(),
                testEventLog2.getDescription()
        );
    }

    @Test
    @DisplayName("Should find all event logs successfully")
    void shouldFindAllEventLogsSuccessfully() {
        // Given
        List<EventLog> eventLogs = Arrays.asList(testEventLog1, testEventLog2);
        List<EventLogResponseDto> expectedResponse = Arrays.asList(testResponseDto1, testResponseDto2);

        when(eventLogRepository.findAll()).thenReturn(eventLogs);
        when(eventLogMapper.toEventLogResponseDtoList(eventLogs)).thenReturn(expectedResponse);

        // When
        List<EventLogResponseDto> result = eventLogService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedResponse, result);
        verify(eventLogRepository).findAll();
        verify(eventLogMapper).toEventLogResponseDtoList(eventLogs);
    }

    @Test
    @DisplayName("Should return empty list when no event logs exist")
    void shouldReturnEmptyListWhenNoEventLogsExist() {
        // Given
        List<EventLog> emptyList = Arrays.asList();
        List<EventLogResponseDto> emptyResponseList = Arrays.asList();

        when(eventLogRepository.findAll()).thenReturn(emptyList);
        when(eventLogMapper.toEventLogResponseDtoList(emptyList)).thenReturn(emptyResponseList);

        // When
        List<EventLogResponseDto> result = eventLogService.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventLogRepository).findAll();
        verify(eventLogMapper).toEventLogResponseDtoList(emptyList);
    }

    @Test
    @DisplayName("Should find events by date range successfully")
    void shouldFindEventsByDateRangeSuccessfully() {
        // Given
        Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endDate = Instant.now();
        List<EventLog> eventLogs = Arrays.asList(testEventLog1);
        List<EventLogResponseDto> expectedResponse = Arrays.asList(testResponseDto1);

        when(eventLogRepository.findByTimestampBetween(startDate, endDate)).thenReturn(eventLogs);
        when(eventLogMapper.toEventLogResponseDtoList(eventLogs)).thenReturn(expectedResponse);

        // When
        List<EventLogResponseDto> result = eventLogService.findEventsByDateRange(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedResponse, result);
        verify(eventLogRepository).findByTimestampBetween(startDate, endDate);
        verify(eventLogMapper).toEventLogResponseDtoList(eventLogs);
    }

    @Test
    @DisplayName("Should return empty list when no events in date range")
    void shouldReturnEmptyListWhenNoEventsInDateRange() {
        // Given
        Instant startDate = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant endDate = Instant.now().minus(5, ChronoUnit.DAYS);
        List<EventLog> emptyList = Arrays.asList();
        List<EventLogResponseDto> emptyResponseList = Arrays.asList();

        when(eventLogRepository.findByTimestampBetween(startDate, endDate)).thenReturn(emptyList);
        when(eventLogMapper.toEventLogResponseDtoList(emptyList)).thenReturn(emptyResponseList);

        // When
        List<EventLogResponseDto> result = eventLogService.findEventsByDateRange(startDate, endDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventLogRepository).findByTimestampBetween(startDate, endDate);
        verify(eventLogMapper).toEventLogResponseDtoList(emptyList);
    }
}

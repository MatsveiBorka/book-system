package com.example.logging.service;

import com.example.logging.dto.EventLogResponseDto;
import com.example.logging.entity.EventLog;
import com.example.logging.mapper.EventLogMapper;
import com.example.logging.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogRepository eventLogRepository;
    private final EventLogMapper eventLogMapper;

    public List<EventLogResponseDto> findAll() {
        List<EventLog> eventLogs = eventLogRepository.findAll();
        return eventLogMapper.toEventLogResponseDtoList(eventLogs);
    }

    public List<EventLogResponseDto> findEventsByDateRange(Instant startDate, Instant endDate) {
        List<EventLog> eventLogs = eventLogRepository.findByTimestampBetween(startDate, endDate);
        return eventLogMapper.toEventLogResponseDtoList(eventLogs);
    }
}

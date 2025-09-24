package com.example.logging.mapper;

import com.example.logging.dto.EventLogResponseDto;
import com.example.logging.entity.EventLog;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventLogMapper {
    EventLogResponseDto toEventLogResponseDto(EventLog eventLog);

    List<EventLogResponseDto> toEventLogResponseDtoList(List<EventLog> eventLogs);
}

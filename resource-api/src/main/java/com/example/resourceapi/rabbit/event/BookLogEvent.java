package com.example.resourceapi.rabbit.event;

import com.example.resourceapi.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookLogEvent implements Serializable {
    private Instant timestamp;
    private String subjectType;
    private EventType eventType;
    private String eventDescription;
}
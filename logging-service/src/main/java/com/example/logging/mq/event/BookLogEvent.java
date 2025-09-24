package com.example.logging.mq.event;

import com.example.logging.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookLogEvent implements Serializable {
    protected Instant timestamp;
    protected String subjectType;
    protected EventType eventType;
    protected String eventDescription;
}
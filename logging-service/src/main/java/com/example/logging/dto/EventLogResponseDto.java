package com.example.logging.dto;

import java.time.Instant;
import java.util.UUID;

public record EventLogResponseDto(UUID id,
                                  Instant timestamp,
                                  String subjectType,
                                  String eventType,
                                  String description)
{}

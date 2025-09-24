package com.example.logging.controller;

import com.example.logging.dto.EventLogResponseDto;
import com.example.logging.entity.EventLog;
import com.example.logging.service.EventLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Event Logs", description = "API for managing and retrieving event logs")
public class EventController {

    private final EventLogService eventLogService;

    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieve all event logs from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all events",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventLog.class)))
    })
    public List<EventLogResponseDto> getAllEvents() {
        return eventLogService.findAll();
    }

    @GetMapping("/range")
    @Operation(summary = "Get events by date range",
               description = "Retrieve event logs within a specified date and time range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events for the specified date range",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventLog.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date format or parameters")
    })
    public List<EventLogResponseDto> getEventsByDateRange(
            @Parameter(description = "Start date and time in ISO 8601 format (e.g., 2025-09-20T00:00:00Z)",
                      required = true, example = "2025-09-20T00:00:00Z")
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,

            @Parameter(description = "End date and time in ISO 8601 format (e.g., 2025-09-22T23:59:59Z)",
                      required = true, example = "2025-09-22T23:59:59Z")
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        return eventLogService.findEventsByDateRange(startDate, endDate);
    }
}

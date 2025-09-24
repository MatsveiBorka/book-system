package com.example.resourceapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.UUID;

@Builder
@Schema(description = "Request data for updating an existing book (all fields except ID are optional)")
public record UpdateBookRequestDto(
        @NotNull(message = "Id must not be null") UUID id,

        @Schema(description = "Updated title of the book", example = "Spring Boot in Action - 2nd Edition")
        JsonNullable<String> title,

        @Schema(description = "Updated author of the book", example = "Craig Walls")
        JsonNullable<String> author,

        @Valid
        @Schema(description = "Updated publication publicationYear", example = "2024", minimum = "1000", maximum = "9999")
        JsonNullable<
                @Min(value = 1000, message = "Minimal value is 1000")
                @Max(value = 9999, message = "Maximum value is 9999") Integer> publicationYear,

        @Schema(description = "Updated description of the book", example = "An updated comprehensive guide to Spring Boot development")
        JsonNullable<String> description
) {
}

package com.example.resourceapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(description = "Response data for a book")
public record CreateBookResponseDto(
        @Schema(description = "Unique identifier of the book", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Title of the book", example = "Spring Boot in Action")
        String title,

        @Schema(description = "Author of the book", example = "Craig Walls")
        String author,

        @Schema(description = "Year the book was published", example = "2023")
        Integer publicationYear,

        @Schema(description = "Description of the book", example = "A comprehensive guide to Spring Boot development")
        String description
) {
}

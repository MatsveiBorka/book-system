package com.example.resourceapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Request data for creating a new book")
public record CreateBookRequestDto(
        @NotBlank(message = "Title is mandatory")
        @Schema(description = "Title of the book", example = "Spring Boot in Action", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "Author of the book", example = "Craig Walls", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String author,

        @Min(1000)
        @Max(9999)
        @Schema(description = "Year the book was published", example = "2023", minimum = "1000", maximum = "9999", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer publicationYear,

        @Schema(description = "Description of the book", example = "A comprehensive guide to Spring Boot development", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description
) {
}
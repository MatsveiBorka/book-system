package com.example.resourceapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Paginated response containing books and pagination metadata")
public record PagedBooksResponseDto(
        @Schema(description = "List of books in the current page")
        List<CreateBookResponseDto> books,

        @Schema(description = "Total number of books matching the criteria", example = "150")
        long totalElements,

        @Schema(description = "Total number of pages", example = "15")
        int totalPages,

        @Schema(description = "Current page number (0-based)", example = "2")
        int currentPage,

        @Schema(description = "Number of items per page", example = "10")
        int pageSize,

        @Schema(description = "Whether there is a next page available", example = "true")
        boolean hasNext,

        @Schema(description = "Whether there is a previous page available", example = "true")
        boolean hasPrevious
) {
}

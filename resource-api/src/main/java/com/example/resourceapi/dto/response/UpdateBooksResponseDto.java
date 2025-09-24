package com.example.resourceapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Response data for bulk book update operations")
public record UpdateBooksResponseDto(
        @Schema(description = "List of successfully updated books")
        List<CreateBookResponseDto> updatedBooks,

        @Schema(description = "Status message about the update operation",
                example = "Books are successfully updated")
        String message,

        @Schema(description = "List of book IDs that were not found and could not be updated")
        List<UUID> notUpdatedIds
) {
}

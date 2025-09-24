package com.example.resourceapi.controller;

import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.dto.response.CreateBookResponseDto;
import com.example.resourceapi.dto.response.PagedBooksResponseDto;
import com.example.resourceapi.dto.response.UpdateBooksResponseDto;
import com.example.resourceapi.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "API for managing books and resources")
public class BookController {
    private final BookService bookService;

    @PostMapping
    @Operation(summary = "Create books", description = "Create one or more new books in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateBookResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid book data provided")
    })
    public List<CreateBookResponseDto> addBooks(
            @Parameter(description = "List of books to create", required = true)
            @RequestBody List<@Valid CreateBookRequestDto> books) {
        return bookService.saveAll(books);
    }

    @GetMapping
    @Operation(summary = "List books", description = "Retrieve books with optional filtering and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters")
    })
    public PagedBooksResponseDto listBooks(
            @PageableDefault(page = 0, size = 10, sort = "title", direction = Sort.Direction.ASC)
            @Parameter(description = "Pagination information") Pageable pageable,
            @Parameter(description = "Filter by book title", example = "Spring Boot Guide")
            @RequestParam(name = "title", required = false) String title,
            @Parameter(description = "Filter by author name", example = "John Doe")
            @RequestParam(name = "author", required = false) String author,
            @Parameter(description = "Filter by publication publicationYear", example = "2023")
            @RequestParam(name = "publicationYear", required = false) Integer publicationYear) {
        return bookService.findBooksWithPagination(pageable, title, author, publicationYear);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Retrieve a specific book by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateBookResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public CreateBookResponseDto getBookById(
            @Parameter(description = "Unique identifier of the book", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable(name = "id") UUID id) {
        return bookService.findById(id);
    }

    @PatchMapping
    @Operation(summary = "Update books", description = "Update specific fields of existing books")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books are updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateBooksResponseDto.class))),
    })
    public UpdateBooksResponseDto updateBook(
            @Parameter(description = "Fields to update", required = true)
            @RequestBody List<@Valid UpdateBookRequestDto> updateRequest) {
        return bookService.updateBooks(updateRequest);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete book", description = "Delete a book by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "Unique identifier of the book to delete", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable(name = "id") UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}

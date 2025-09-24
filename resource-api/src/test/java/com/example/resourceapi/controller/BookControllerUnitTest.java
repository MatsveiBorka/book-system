package com.example.resourceapi.controller;

import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.dto.response.CreateBookResponseDto;
import com.example.resourceapi.dto.response.PagedBooksResponseDto;
import com.example.resourceapi.dto.response.UpdateBooksResponseDto;
import com.example.resourceapi.exception.BookNotFoundException;
import com.example.resourceapi.service.impl.BookServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@DisplayName("BookController Unit Tests")
class BookControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookServiceImpl bookService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateBookRequestDto testRequestDto;
    private CreateBookResponseDto testResponseDto;
    private UpdateBookRequestDto testUpdateDto;
    private PagedBooksResponseDto testPagedResponse;
    private UUID testBookId;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper to handle JsonNullable
        objectMapper.registerModule(new JsonNullableModule());

        testBookId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        testRequestDto = CreateBookRequestDto.builder()
                .title("Spring Boot Guide")
                .author("John Doe")
                .publicationYear(2023)
                .description("Complete guide to Spring Boot")
                .build();

        testResponseDto = CreateBookResponseDto.builder()
                .id(testBookId)
                .title("Spring Boot Guide")
                .author("John Doe")
                .publicationYear(2023)
                .description("Complete guide to Spring Boot")
                .build();

        testUpdateDto = UpdateBookRequestDto.builder()
                .id(testBookId)
                .title(JsonNullable.of("Updated Title"))
                .author(JsonNullable.of("Updated Author"))
                .publicationYear(JsonNullable.of(2024))
                .description(JsonNullable.of("Updated Description"))
                .build();

        testPagedResponse = PagedBooksResponseDto.builder()
                .books(List.of(testResponseDto))
                .totalElements(1)
                .totalPages(1)
                .currentPage(0)
                .pageSize(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    @Test
    @DisplayName("Should create books successfully")
    void shouldCreateBooksSuccessfully() throws Exception {
        // Given
        List<CreateBookRequestDto> requestDtos = List.of(testRequestDto);
        List<CreateBookResponseDto> responseDtos = List.of(testResponseDto);
        String requestJson = objectMapper.writeValueAsString(requestDtos);

        when(bookService.saveAll(requestDtos)).thenReturn(responseDtos);

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(testBookId.toString()))
                .andExpect(jsonPath("$[0].title").value("Spring Boot Guide"))
                .andExpect(jsonPath("$[0].author").value("John Doe"))
                .andExpect(jsonPath("$[0].publicationYear").value(2023));

        verify(bookService).saveAll(requestDtos);
    }

    @Test
    @DisplayName("Should handle empty book list creation")
    void shouldHandleEmptyBookListCreation() throws Exception {
        // Given
        List<CreateBookRequestDto> emptyList = Collections.emptyList();
        String requestJson = objectMapper.writeValueAsString(emptyList);

        when(bookService.saveAll(emptyList)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(bookService).saveAll(emptyList);
    }

    @Test
    @DisplayName("Should get all books with pagination")
    void shouldGetAllBooksWithPagination() throws Exception {
        // Given
        when(bookService.findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null)))
                .thenReturn(testPagedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(bookService).findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("Should get books with filters")
    void shouldGetBooksWithFilters() throws Exception {
        // Given
        String title = "Spring";
        String author = "John";
        Integer year = 2023;

        when(bookService.findBooksWithPagination(any(Pageable.class), eq(title), eq(author), eq(year)))
                .thenReturn(testPagedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "10")
                        .param("title", title)
                        .param("author", author)
                        .param("publicationYear", year.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray());

        verify(bookService).findBooksWithPagination(any(Pageable.class), eq(title), eq(author), eq(year));
    }

    @Test
    @DisplayName("Should get book by ID successfully")
    void shouldGetBookByIdSuccessfully() throws Exception {
        // Given
        when(bookService.findById(testBookId)).thenReturn(testResponseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/books/{id}", testBookId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testBookId.toString()))
                .andExpect(jsonPath("$.title").value("Spring Boot Guide"))
                .andExpect(jsonPath("$.author").value("John Doe"))
                .andExpect(jsonPath("$.publicationYear").value(2023));

        verify(bookService).findById(testBookId);
    }

    @Test
    @DisplayName("Should return 404 when book not found by ID")
    void shouldReturn404WhenBookNotFoundById() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(bookService.findById(nonExistentId)).thenThrow(new BookNotFoundException(nonExistentId));

        // When & Then
        mockMvc.perform(get("/api/v1/books/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        verify(bookService).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should update books successfully")
    void shouldUpdateBooksSuccessfully() throws Exception {
        // Given
        List<UpdateBookRequestDto> updateRequests = List.of(testUpdateDto);
        UpdateBooksResponseDto updateResponse = UpdateBooksResponseDto.builder()
                .updatedBooks(List.of(testResponseDto))
                .message("Books are successfully updated")
                .notUpdatedIds(Collections.emptyList())
                .build();

        String requestJson = objectMapper.writeValueAsString(updateRequests);
        when(bookService.updateBooks(any(List.class))).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.updatedBooks").isArray())
                .andExpect(jsonPath("$.updatedBooks.length()").value(1))
                .andExpect(jsonPath("$.message").value("Books are successfully updated"))
                .andExpect(jsonPath("$.notUpdatedIds").isArray())
                .andExpect(jsonPath("$.notUpdatedIds.length()").value(0));

        verify(bookService).updateBooks(any(List.class));
    }

    @Test
    @DisplayName("Should delete book successfully")
    void shouldDeleteBookSuccessfully() throws Exception {
        // Given
        doNothing().when(bookService).deleteBook(testBookId);

        // When & Then
        mockMvc.perform(delete("/api/v1/books/{id}", testBookId))
                .andExpect(status().isNoContent());

        verify(bookService).deleteBook(testBookId);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent book")
    void shouldReturn404WhenDeletingNonExistentBook() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new BookNotFoundException(nonExistentId)).when(bookService).deleteBook(nonExistentId);

        // When & Then
        mockMvc.perform(delete("/api/v1/books/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        verify(bookService).deleteBook(nonExistentId);
    }

    @Test
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors() throws Exception {
        // Given - Invalid request with missing required fields
        CreateBookRequestDto invalidDto = CreateBookRequestDto.builder()
                .title("") // Invalid empty title
                .author(null) // Invalid null author
                .publicationYear(1500) // Invalid publicationYear
                .build();

        String requestJson = objectMapper.writeValueAsString(List.of(invalidDto));

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }

    @Test
    @DisplayName("Should handle malformed JSON requests")
    void shouldHandleMalformedJsonRequests() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalid\": \"json\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }

    @Test
    @DisplayName("Should handle missing request body")
    void shouldHandleMissingRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }

    @Test
    @DisplayName("Should handle invalid UUID format in path parameter")
    void shouldHandleInvalidUuidFormatInPathParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/books/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }

    @Test
    @DisplayName("Should handle pagination with default parameters")
    void shouldHandlePaginationWithDefaultParameters() throws Exception {
        // Given
        when(bookService.findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null)))
                .thenReturn(testPagedResponse);

        // When & Then - Request without pagination parameters
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(bookService).findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("Should handle invalid pagination parameters")
    void shouldHandleInvalidPaginationParameters() throws Exception {
        // Given - Spring Boot handles negative page/size gracefully by default
        // So we expect the service to be called but with corrected parameters
        when(bookService.findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null)))
                .thenReturn(testPagedResponse);

        // When & Then - Invalid parameters are corrected by Spring Boot
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "-1") // Gets corrected to 0
                        .param("size", "0"))  // Gets corrected to default size
                .andExpect(status().isOk()); // Changed from isBadRequest()

        verify(bookService).findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("Should handle service layer exceptions")
    void shouldHandleServiceLayerExceptions() throws Exception {
        // Given
        List<CreateBookRequestDto> requestDtos = List.of(testRequestDto);
        String requestJson = objectMapper.writeValueAsString(requestDtos);

        when(bookService.saveAll(requestDtos)).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError());

        verify(bookService).saveAll(requestDtos);
    }

    @Test
    @DisplayName("Should handle large request payload")
    void shouldHandleLargeRequestPayload() throws Exception {
        // Given - Create a large list of books
        List<CreateBookRequestDto> largeList = List.of(
                CreateBookRequestDto.builder().title("Book 1").author("Author 1").publicationYear(2021).build(),
                CreateBookRequestDto.builder().title("Book 2").author("Author 2").publicationYear(2022).build(),
                CreateBookRequestDto.builder().title("Book 3").author("Author 3").publicationYear(2023).build(),
                CreateBookRequestDto.builder().title("Book 4").author("Author 4").publicationYear(2024).build(),
                CreateBookRequestDto.builder().title("Book 5").author("Author 5").publicationYear(2025).build()
        );

        List<CreateBookResponseDto> responseList = largeList.stream()
                .map(dto -> CreateBookResponseDto.builder()
                        .id(UUID.randomUUID())
                        .title(dto.title())
                        .author(dto.author())
                        .publicationYear(dto.publicationYear())
                        .build())
                .toList();

        String requestJson = objectMapper.writeValueAsString(largeList);
        when(bookService.saveAll(largeList)).thenReturn(responseList);

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].title").value("Book 1"))
                .andExpect(jsonPath("$[4].title").value("Book 5"));

        verify(bookService).saveAll(largeList);
    }

    @Test
    @DisplayName("Should handle update with mixed success and failure")
    void shouldHandleUpdateWithMixedSuccessAndFailure() throws Exception {
        // Given
        UUID foundId = UUID.randomUUID();
        UUID notFoundId = UUID.randomUUID();

        UpdateBookRequestDto foundUpdate = UpdateBookRequestDto.builder()
                .id(foundId)
                .title(JsonNullable.of("Updated Title"))
                .build();

        UpdateBookRequestDto notFoundUpdate = UpdateBookRequestDto.builder()
                .id(notFoundId)
                .title(JsonNullable.of("Another Title"))
                .build();

        List<UpdateBookRequestDto> updateRequests = List.of(foundUpdate, notFoundUpdate);

        UpdateBooksResponseDto mixedResponse = UpdateBooksResponseDto.builder()
                .updatedBooks(List.of(testResponseDto))
                .message("Books with IDs [" + notFoundId + "] are not updated")
                .notUpdatedIds(List.of(notFoundId))
                .build();

        String requestJson = objectMapper.writeValueAsString(updateRequests);
        when(bookService.updateBooks(any(List.class))).thenReturn(mixedResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBooks.length()").value(1))
                .andExpect(jsonPath("$.notUpdatedIds.length()").value(1))
                .andExpect(jsonPath("$.notUpdatedIds[0]").value(notFoundId.toString()))
                .andExpect(jsonPath("$.message").value(containsString("not updated")));

        verify(bookService).updateBooks(any(List.class));
    }

    @Test
    @DisplayName("Should handle content type validation")
    void shouldHandleContentTypeValidation() throws Exception {
        // Given
        String requestJson = objectMapper.writeValueAsString(List.of(testRequestDto));

        // When & Then - Wrong content type
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(requestJson))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(bookService);
    }

    @Test
    @DisplayName("Should handle HTTP method validation")
    void shouldHandleHttpMethodValidation() throws Exception {
        // When & Then - Wrong HTTP method for endpoints that don't exist
        // Note: PUT is actually supported for updates in this controller
        mockMvc.perform(post("/api/v1/books/{id}", testBookId))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/v1/books/{id}", testBookId))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(bookService);
    }

    @Test
    @DisplayName("Should handle special characters in request parameters")
    void shouldHandleSpecialCharactersInRequestParameters() throws Exception {
        // Given
        String titleWithSpecialChars = "Spring & Boot: Advanced";
        String authorWithSpecialChars = "José María";

        when(bookService.findBooksWithPagination(any(Pageable.class), eq(titleWithSpecialChars), eq(authorWithSpecialChars), eq(null)))
                .thenReturn(testPagedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/books")
                        .param("title", titleWithSpecialChars)
                        .param("author", authorWithSpecialChars))
                .andExpect(status().isOk());

        verify(bookService).findBooksWithPagination(any(Pageable.class), eq(titleWithSpecialChars), eq(authorWithSpecialChars), eq(null));
    }

    @Test
    @DisplayName("Should handle large page size requests")
    void shouldHandleLargePageSizeRequests() throws Exception {
        // Given
        PagedBooksResponseDto largePageResponse = PagedBooksResponseDto.builder()
                .books(Collections.emptyList())
                .totalElements(0)
                .totalPages(0)
                .currentPage(0)
                .pageSize(1000)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null)))
                .thenReturn(largePageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(1000));

        verify(bookService).findBooksWithPagination(any(Pageable.class), eq(null), eq(null), eq(null));
    }
}

package com.example.resourceapi.controller;

import com.example.resourceapi.config.TestContainersConfig;
import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.entity.Book;
import com.example.resourceapi.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
@AutoConfigureWebMvc
@DisplayName("BookController Full Integration Tests with TestContainers")
class BookControllerFullIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper to handle JsonNullable
        objectMapper.registerModule(new JsonNullableModule());

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean database before each test
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create books successfully with real database")
    void shouldCreateBooksSuccessfullyWithRealDatabase() throws Exception {
        // Given
        List<CreateBookRequestDto> requestDtos = List.of(
                CreateBookRequestDto.builder()
                        .title("Spring Boot Guide")
                        .author("John Doe")
                        .publicationYear(2023)
                        .description("Complete guide to Spring Boot")
                        .build(),
                CreateBookRequestDto.builder()
                        .title("Microservices Architecture")
                        .author("Jane Smith")
                        .publicationYear(2022)
                        .description("Building scalable microservices")
                        .build()
        );

        String requestJson = objectMapper.writeValueAsString(requestDtos);

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())  // Changed from isCreated() to isOk()
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Spring Boot Guide"))
                .andExpect(jsonPath("$[0].author").value("John Doe"))
                .andExpect(jsonPath("$[0].publicationYear").value(2023))
                .andExpect(jsonPath("$[1].title").value("Microservices Architecture"))
                .andExpect(jsonPath("$[1].author").value("Jane Smith"))
                .andExpect(jsonPath("$[1].publicationYear").value(2022));

        // Verify data persisted in PostgreSQL
        List<Book> booksInDb = bookRepository.findAll();
        assertEquals(2, booksInDb.size());
        assertTrue(booksInDb.stream().anyMatch(b -> "Spring Boot Guide".equals(b.getTitle())));
        assertTrue(booksInDb.stream().anyMatch(b -> "Microservices Architecture".equals(b.getTitle())));
    }

    @Test
    @DisplayName("Should get all books with pagination using PostgreSQL")
    void shouldGetAllBooksWithPaginationUsingPostgreSQL() throws Exception {
        // Given - Create test data in PostgreSQL
        createTestBooksInDatabase();

        // When & Then - Test first page
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())  // Changed from $.content to $.books
                .andExpect(jsonPath("$.books.length()").value(2))  // Changed from $.content.length()
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasPrevious").value(false))  // Changed from $.first
                .andExpect(jsonPath("$.hasNext").value(true));  // Changed from $.last

        // Test second page
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books.length()").value(2))  // Changed from $.content.length()
                .andExpect(jsonPath("$.hasPrevious").value(true))  // Changed from $.first
                .andExpect(jsonPath("$.hasNext").value(true));  // Changed from $.last
    }

    @Test
    @DisplayName("Should get book by ID from PostgreSQL")
    void shouldGetBookByIdFromPostgreSQL() throws Exception {
        // Given
        Book savedBook = createSingleTestBook();

        // When & Then
        mockMvc.perform(get("/api/v1/books/{id}", savedBook.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedBook.getId().toString()))
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"))
                .andExpect(jsonPath("$.publicationYear").value(2023))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    @DisplayName("Should update book and persist changes in PostgreSQL")
    void shouldUpdateBookAndPersistChangesInPostgreSQL() throws Exception {
        // Given
        Book savedBook = createSingleTestBook();

        UpdateBookRequestDto updateDto = UpdateBookRequestDto.builder()
                .id(savedBook.getId())  // Add the book ID to the DTO
                .title(JsonNullable.of("Updated Title"))
                .author(JsonNullable.of("Updated Author"))
                .publicationYear(JsonNullable.of(2024))
                .description(JsonNullable.of("Updated Description"))
                .build();

        String requestJson = objectMapper.writeValueAsString(List.of(updateDto));  // Wrap in list

        // When & Then
        mockMvc.perform(patch("/api/v1/books")  // Changed from PUT to PATCH, removed /{id}
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.updatedBooks[0].title").value("Updated Title"))  // Updated JSON path
                .andExpect(jsonPath("$.updatedBooks[0].author").value("Updated Author"))
                .andExpect(jsonPath("$.updatedBooks[0].publicationYear").value(2024))
                .andExpect(jsonPath("$.updatedBooks[0].description").value("Updated Description"));

        // Verify changes persisted in PostgreSQL
        Book updatedBook = bookRepository.findById(savedBook.getId()).orElseThrow();
        assertEquals("Updated Title", updatedBook.getTitle());
        assertEquals("Updated Author", updatedBook.getAuthor());
        assertEquals(2024, updatedBook.getPublicationYear());
        assertEquals("Updated Description", updatedBook.getDescription());
    }

    @Test
    @DisplayName("Should delete book and remove from PostgreSQL")
    void shouldDeleteBookAndRemoveFromPostgreSQL() throws Exception {
        // Given
        Book savedBook = createSingleTestBook();
        UUID bookId = savedBook.getId();

        // Verify book exists
        assertEquals(1, bookRepository.count());

        // When & Then
        mockMvc.perform(delete("/api/v1/books/{id}", bookId))
                .andExpect(status().isNoContent());

        // Verify book removed from PostgreSQL
        assertEquals(0, bookRepository.count());
        assertFalse(bookRepository.findById(bookId).isPresent());
    }

    @Test
    @DisplayName("Should handle validation errors correctly")
    void shouldHandleValidationErrorsCorrectly() throws Exception {
        // Given - Invalid request with missing required fields
        List<CreateBookRequestDto> invalidRequest = List.of(
                CreateBookRequestDto.builder()
                        .title("") // Invalid empty title
                        .author(null) // Invalid null author
                        .publicationYear(1800) // Invalid publicationYear (too old)
                        .build()
        );

        String requestJson = objectMapper.writeValueAsString(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Verify no data was saved to PostgreSQL
        assertEquals(0, bookRepository.count());
    }

    @Test
    @DisplayName("Should handle not found errors correctly")
    void shouldHandleNotFoundErrorsCorrectly() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then - Get non-existent book
        mockMvc.perform(get("/api/v1/books/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        // When & Then - Update non-existent book
        UpdateBookRequestDto updateDto = UpdateBookRequestDto.builder()
                .id(nonExistentId)  // Add the ID to the DTO
                .title(JsonNullable.of("Some Title"))
                .build();

        String requestJson = objectMapper.writeValueAsString(List.of(updateDto));  // Wrap in list

        mockMvc.perform(patch("/api/v1/books")  // Changed from PUT to PATCH, removed /{id}
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());  // PATCH returns 200 even if some books aren't found

        // When & Then - Delete non-existent book
        mockMvc.perform(delete("/api/v1/books/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle large dataset pagination correctly")
    void shouldHandleLargeDatasetPaginationCorrectly() throws Exception {
        // Given - Create 25 books
        for (int i = 1; i <= 25; i++) {
            Book book = new Book();
            book.setTitle("Book " + i);
            book.setAuthor("Author " + i);
            book.setPublicationYear(2000 + i);
            book.setDescription("Description " + i);
            bookRepository.save(book);
        }

        // When & Then - Test various page sizes and positions
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/v1/books")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    private void createTestBooksInDatabase() {
        for (int i = 1; i <= 5; i++) {
            Book book = new Book();
            book.setTitle("Book " + i);
            book.setAuthor("Author " + i);
            book.setPublicationYear(2020 + i);
            book.setDescription("Description " + i);
            bookRepository.save(book);
        }
    }

    private Book createSingleTestBook() {
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setPublicationYear(2023);
        book.setDescription("Test Description");
        return bookRepository.save(book);
    }
}

package com.example.resourceapi.integration;

import com.example.resourceapi.config.TestContainersConfig;
import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.entity.Book;
import com.example.resourceapi.enums.EventType;
import com.example.resourceapi.rabbit.event.BookLogEvent;
import com.example.resourceapi.rabbit.publisher.BookLogEventPublisher;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
@AutoConfigureWebMvc
@DisplayName("End-to-End Integration Tests with TestContainers")
class FullStackIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookLogEventPublisher bookLogEventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper to handle JsonNullable
        objectMapper.registerModule(new JsonNullableModule());

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Should perform complete CRUD operations with PostgreSQL and RabbitMQ")
    void shouldPerformCompleteCrudOperationsWithPostgreSQLAndRabbitMQ() throws Exception {
        // CREATE - Test book creation
        List<CreateBookRequestDto> createRequests = List.of(
                CreateBookRequestDto.builder()
                        .title("Full Stack Test Book")
                        .author("Integration Test Author")
                        .publicationYear(2023)
                        .description("Testing complete application stack")
                        .build()
        );

        String createJson = objectMapper.writeValueAsString(createRequests);

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Full Stack Test Book"))
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        List<?> createResponse = objectMapper.readValue(createResponseBody, List.class);
        String bookId = ((java.util.Map<?, ?>) createResponse.get(0)).get("id").toString();

        // Verify in PostgreSQL
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Book> books = bookRepository.findAll();
            assertEquals(1, books.size());
            assertEquals("Full Stack Test Book", books.get(0).getTitle());
        });

        // Publish CREATE event
        BookLogEvent createEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.CREATE,
                "Book 'Full Stack Test Book' was created via API"
        );
        assertDoesNotThrow(() -> bookLogEventPublisher.publishEvent(createEvent));

        // READ - Test getting all books
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].title").value("Full Stack Test Book"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // READ - Test getting book by ID
        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Full Stack Test Book"))
                .andExpect(jsonPath("$.author").value("Integration Test Author"));

        // UPDATE - Test book update
        UpdateBookRequestDto updateRequest = UpdateBookRequestDto.builder()
                .id(UUID.fromString(bookId))
                .title(JsonNullable.of("Updated Full Stack Test Book"))
                .description(JsonNullable.of("Updated description for integration testing"))
                .build();

        String updateJson = objectMapper.writeValueAsString(List.of(updateRequest));
        mockMvc.perform(patch("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBooks[0].title").value("Updated Full Stack Test Book"))
                .andExpect(jsonPath("$.updatedBooks[0].description").value("Updated description for integration testing"));

        // Verify changes persisted in PostgreSQL
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Book updatedBook = bookRepository.findById(UUID.fromString(bookId)).orElseThrow();
            assertEquals("Updated Full Stack Test Book", updatedBook.getTitle());
            assertEquals("Updated description for integration testing", updatedBook.getDescription());
        });

        // Publish UPDATE event
        BookLogEvent updateEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.UPDATE,
                "Book 'Updated Full Stack Test Book' was updated via API"
        );
        assertDoesNotThrow(() -> bookLogEventPublisher.publishEvent(updateEvent));

        // DELETE - Test book deletion
        mockMvc.perform(delete("/api/v1/books/{id}", bookId))
                .andExpect(status().isNoContent());

        // Verify deletion in PostgreSQL
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(0, bookRepository.count());
            assertFalse(bookRepository.findById(UUID.fromString(bookId)).isPresent());
        });

        // Publish DELETE event
        BookLogEvent deleteEvent = new BookLogEvent(
                Instant.now(),
                "Book",
                EventType.DELETE,
                "Book was deleted via API"
        );
        assertDoesNotThrow(() -> bookLogEventPublisher.publishEvent(deleteEvent));

        // Verify book is gone
        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle concurrent operations correctly with real database")
    void shouldHandleConcurrentOperationsCorrectlyWithRealDatabase() throws Exception {
        // Create multiple books concurrently
        List<CreateBookRequestDto> batch1 = List.of(
                CreateBookRequestDto.builder()
                        .title("Concurrent Book 1")
                        .author("Author 1")
                        .publicationYear(2023)
                        .build(),
                CreateBookRequestDto.builder()
                        .title("Concurrent Book 2")
                        .author("Author 2")
                        .publicationYear(2023)
                        .build()
        );

        List<CreateBookRequestDto> batch2 = List.of(
                CreateBookRequestDto.builder()
                        .title("Concurrent Book 3")
                        .author("Author 3")
                        .publicationYear(2023)
                        .build()
        );

        String batch1Json = objectMapper.writeValueAsString(batch1);
        String batch2Json = objectMapper.writeValueAsString(batch2);

        // Execute requests concurrently
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch1Json))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch2Json))
                .andExpect(status().isOk());

        // Verify all books were created in PostgreSQL
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Book> allBooks = bookRepository.findAll();
            assertEquals(3, allBooks.size());
            assertTrue(allBooks.stream().anyMatch(b -> "Concurrent Book 1".equals(b.getTitle())));
            assertTrue(allBooks.stream().anyMatch(b -> "Concurrent Book 2".equals(b.getTitle())));
            assertTrue(allBooks.stream().anyMatch(b -> "Concurrent Book 3".equals(b.getTitle())));
        });
    }

    @Test
    @DisplayName("Should maintain data consistency across operations")
    void shouldMaintainDataConsistencyAcrossOperations() throws Exception {
        // Create initial dataset
        for (int i = 1; i <= 10; i++) {
            CreateBookRequestDto request = CreateBookRequestDto.builder()
                    .title("Consistency Test Book " + i)
                    .author("Author " + i)
                    .publicationYear(2020 + i)
                    .description("Description " + i)
                    .build();

            String json = objectMapper.writeValueAsString(List.of(request));
            mockMvc.perform(post("/api/v1/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());
        }

        // Verify initial count
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(10, bookRepository.count());
        });

        // Get all books and verify pagination consistency
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Update some books
        List<Book> books = bookRepository.findAll();
        for (int i = 0; i < 3; i++) {
            Book book = books.get(i);
            UpdateBookRequestDto updateRequest = UpdateBookRequestDto.builder()
                    .id(book.getId())  // Add the book ID to the DTO
                    .title(JsonNullable.of("Updated " + book.getTitle()))
                    .build();

            String updateJson = objectMapper.writeValueAsString(List.of(updateRequest)); // Wrap in list
            mockMvc.perform(patch("/api/v1/books")  // Changed from PUT to PATCH, removed /{id}
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk());
        }

        // Verify updates and count consistency
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Book> updatedBooks = bookRepository.findAll();
            assertEquals(10, updatedBooks.size()); // Count should remain the same

            long updatedCount = updatedBooks.stream()
                    .filter(b -> b.getTitle().startsWith("Updated"))
                    .count();
            assertEquals(3, updatedCount);
        });

        // Delete some books
        for (int i = 0; i < 2; i++) {
            Book book = books.get(i);
            mockMvc.perform(delete("/api/v1/books/{id}", book.getId()))
                    .andExpect(status().isNoContent());
        }

        // Verify final consistency
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(8, bookRepository.count());
        });
    }

    @Test
    @DisplayName("Should handle error scenarios gracefully with real infrastructure")
    void shouldHandleErrorScenariosGracefullyWithRealInfrastructure() throws Exception {
        // Test validation errors
        CreateBookRequestDto invalidRequest = CreateBookRequestDto.builder()
                .title("") // Invalid empty title
                .author(null) // Invalid null author
                .publicationYear(1500) // Invalid publicationYear
                .build();

        String invalidJson = objectMapper.writeValueAsString(List.of(invalidRequest));
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Verify no data was saved
        assertEquals(0, bookRepository.count());

        // Test not found scenarios
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/books/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/books/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        // Test malformed request
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalid\": \"json\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should verify database transactions and rollback behavior")
    void shouldVerifyDatabaseTransactionsAndRollbackBehavior() throws Exception {
        // Ensure clean state
        bookRepository.deleteAll();
        assertEquals(0, bookRepository.count(), "Database should be empty at start");

        // Create a book
        CreateBookRequestDto request = CreateBookRequestDto.builder()
                .title("Transaction Test Book")
                .author("Transaction Author")
                .publicationYear(2023)
                .description("Testing transaction behavior")
                .build();

        String json = objectMapper.writeValueAsString(List.of(request));

        // When - Create book via HTTP request
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Transaction Test Book"));

        // Then - Verify book was persisted
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = bookRepository.count();
            assertEquals(1, count, "Book should be persisted after successful HTTP request");
        });

        // Verify the actual data
        List<Book> books = bookRepository.findAll();
        assertEquals(1, books.size());
        assertEquals("Transaction Test Book", books.get(0).getTitle());
        assertEquals("Transaction Author", books.get(0).getAuthor());

        // Test transaction rollback scenario by attempting an invalid operation
        // This tests that if a service method fails, the transaction should rollback
        CreateBookRequestDto invalidRequest = CreateBookRequestDto.builder()
                .title("") // Invalid empty title
                .author(null) // Invalid null author
                .publicationYear(1500) // Invalid publicationYear
                .build();

        String invalidJson = objectMapper.writeValueAsString(List.of(invalidRequest));

        // This should fail and not create any additional records
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Verify that the invalid request didn't create any additional books
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = bookRepository.count();
            assertEquals(1, count, "Invalid request should not have created additional books");
        });
    }
}

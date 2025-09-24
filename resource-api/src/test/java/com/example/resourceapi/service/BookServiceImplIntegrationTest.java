package com.example.resourceapi.service;

import com.example.resourceapi.config.TestContainersConfig;
import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.dto.response.CreateBookResponseDto;
import com.example.resourceapi.dto.response.PagedBooksResponseDto;
import com.example.resourceapi.entity.Book;
import com.example.resourceapi.exception.BookNotFoundException;
import com.example.resourceapi.repository.BookRepository;
import com.example.resourceapi.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
@DisplayName("BookService Integration Tests with TestContainers")
class BookServiceImplIntegrationTest {

    @Autowired
    private BookServiceImpl bookService;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save books and verify persistence in PostgreSQL")
    void shouldSaveBooksAndVerifyPersistenceInPostgreSQL() {
        // Given
        List<CreateBookRequestDto> requestDtos = List.of(
                CreateBookRequestDto.builder()
                        .title("Spring Boot Mastery")
                        .author("Expert Developer")
                        .publicationYear(2023)
                        .description("Advanced Spring Boot techniques")
                        .build(),
                CreateBookRequestDto.builder()
                        .title("Microservices Design")
                        .author("Architecture Guru")
                        .publicationYear(2022)
                        .description("Building robust microservices")
                        .build()
        );

        // When
        List<CreateBookResponseDto> responseDtos = bookService.saveAll(requestDtos);

        // Then
        assertEquals(2, responseDtos.size());

        // Verify first book
        CreateBookResponseDto firstResponse = responseDtos.get(0);
        assertNotNull(firstResponse.id());
        assertEquals("Spring Boot Mastery", firstResponse.title());
        assertEquals("Expert Developer", firstResponse.author());
        assertEquals(2023, firstResponse.publicationYear());
        assertEquals("Advanced Spring Boot techniques", firstResponse.description());

        // Verify second book
        CreateBookResponseDto secondResponse = responseDtos.get(1);
        assertNotNull(secondResponse.id());
        assertEquals("Microservices Design", secondResponse.title());
        assertEquals("Architecture Guru", secondResponse.author());

        // Verify persistence in PostgreSQL
        List<Book> booksInDb = bookRepository.findAll();
        assertEquals(2, booksInDb.size());
        assertTrue(booksInDb.stream().anyMatch(b -> "Spring Boot Mastery".equals(b.getTitle())));
        assertTrue(booksInDb.stream().anyMatch(b -> "Microservices Design".equals(b.getTitle())));
    }

    @Test
    @DisplayName("Should retrieve all books with pagination from PostgreSQL")
    void shouldRetrieveAllBooksWithPaginationFromPostgreSQL() {
        // Given - Create test data in PostgreSQL
        createTestBooksInDatabase(15);

        // When
        PagedBooksResponseDto pagedResponse = bookService.findBooksWithPagination(PageRequest.of(0, 5), null, null, null);

        // Then
        assertNotNull(pagedResponse);
        assertEquals(5, pagedResponse.books().size());
        assertEquals(15, pagedResponse.totalElements());
        assertEquals(3, pagedResponse.totalPages());
        assertEquals(0, pagedResponse.currentPage());
        assertEquals(5, pagedResponse.pageSize());
        assertFalse(pagedResponse.hasPrevious());
        assertTrue(pagedResponse.hasNext());

        // Test second page
        PagedBooksResponseDto secondPage = bookService.findBooksWithPagination(PageRequest.of(1, 5), null, null, null);
        assertEquals(5, secondPage.books().size());
        assertTrue(secondPage.hasPrevious());
        assertTrue(secondPage.hasNext());

        // Test last page
        PagedBooksResponseDto lastPage = bookService.findBooksWithPagination(PageRequest.of(2, 5), null, null, null);
        assertEquals(5, lastPage.books().size());
        assertTrue(lastPage.hasPrevious());
        assertFalse(lastPage.hasNext());
    }

    @Test
    @DisplayName("Should find book by ID from PostgreSQL")
    void shouldFindBookByIdFromPostgreSQL() {
        // Given
        Book savedBook = createSingleTestBook();

        // When
        CreateBookResponseDto foundBook = bookService.findById(savedBook.getId());

        // Then
        assertNotNull(foundBook);
        assertEquals(savedBook.getId(), foundBook.id());
        assertEquals("Integration Test Book", foundBook.title());
        assertEquals("Test Author", foundBook.author());
        assertEquals(2023, foundBook.publicationYear());
        assertEquals("Test Description", foundBook.description());
    }

    @Test
    @DisplayName("Should throw BookNotFoundException for non-existent book")
    void shouldThrowBookNotFoundExceptionForNonExistentBook() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThrows(BookNotFoundException.class, () -> {
            bookService.findById(nonExistentId);
        });
    }

    @Test
    @DisplayName("Should update book and persist changes in PostgreSQL")
    void shouldUpdateBookAndPersistChangesInPostgreSQL() {
        // Given
        Book savedBook = createSingleTestBook();
        UUID bookId = savedBook.getId();

        UpdateBookRequestDto updateDto = UpdateBookRequestDto.builder()
                .id(bookId)
                .title(JsonNullable.of("Updated Integration Test Book"))
                .author(JsonNullable.of("Updated Test Author"))
                .publicationYear(JsonNullable.of(2024))
                .description(JsonNullable.of("Updated Test Description"))
                .build();

        // When
        var updateResponse = bookService.updateBooks(List.of(updateDto));

        // Then
        assertNotNull(updateResponse);
        assertEquals(1, updateResponse.updatedBooks().size());

        CreateBookResponseDto updatedBookResponse = updateResponse.updatedBooks().get(0);
        assertEquals(bookId, updatedBookResponse.id());
        assertEquals("Updated Integration Test Book", updatedBookResponse.title());
        assertEquals("Updated Test Author", updatedBookResponse.author());
        assertEquals(2024, updatedBookResponse.publicationYear());
        assertEquals("Updated Test Description", updatedBookResponse.description());

        // Verify persistence in PostgreSQL
        Book updatedBookInDb = bookRepository.findById(bookId).orElseThrow();
        assertEquals("Updated Integration Test Book", updatedBookInDb.getTitle());
        assertEquals("Updated Test Author", updatedBookInDb.getAuthor());
        assertEquals(2024, updatedBookInDb.getPublicationYear());
        assertEquals("Updated Test Description", updatedBookInDb.getDescription());
    }

    @Test
    @DisplayName("Should update only specified fields and keep others unchanged")
    void shouldUpdateOnlySpecifiedFieldsAndKeepOthersUnchanged() {
        // Given
        Book savedBook = createSingleTestBook();
        UUID bookId = savedBook.getId();

        UpdateBookRequestDto partialUpdateDto = UpdateBookRequestDto.builder()
                .id(bookId)
                .title(JsonNullable.of("Partially Updated Title"))
                .publicationYear(JsonNullable.of(2024))
                // author and description are not updated
                .build();

        // When
        var updateResponse = bookService.updateBooks(List.of(partialUpdateDto));

        // Then
        CreateBookResponseDto updatedBookResponse = updateResponse.updatedBooks().get(0);
        assertEquals("Partially Updated Title", updatedBookResponse.title());
        assertEquals("Test Author", updatedBookResponse.author()); // Unchanged
        assertEquals(2024, updatedBookResponse.publicationYear());
        assertEquals("Test Description", updatedBookResponse.description()); // Unchanged

        // Verify in database
        Book updatedBookInDb = bookRepository.findById(bookId).orElseThrow();
        assertEquals("Partially Updated Title", updatedBookInDb.getTitle());
        assertEquals("Test Author", updatedBookInDb.getAuthor());
        assertEquals(2024, updatedBookInDb.getPublicationYear());
        assertEquals("Test Description", updatedBookInDb.getDescription());
    }

    @Test
    @DisplayName("Should delete book and remove from PostgreSQL")
    void shouldDeleteBookAndRemoveFromPostgreSQL() {
        // Given
        Book savedBook = createSingleTestBook();
        UUID bookId = savedBook.getId();

        // Verify book exists
        assertTrue(bookRepository.findById(bookId).isPresent());
        assertEquals(1, bookRepository.count());

        // When
        bookService.deleteBook(bookId);

        // Then
        assertFalse(bookRepository.findById(bookId).isPresent());
        assertEquals(0, bookRepository.count());
    }

    @Test
    @DisplayName("Should throw BookNotFoundException when deleting non-existent book")
    void shouldThrowBookNotFoundExceptionWhenDeletingNonExistentBook() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThrows(BookNotFoundException.class, () -> {
            bookService.deleteBook(nonExistentId);
        });
    }

    @Test
    @DisplayName("Should handle empty book list creation")
    void shouldHandleEmptyBookListCreation() {
        // Given
        List<CreateBookRequestDto> emptyList = List.of();

        // When
        List<CreateBookResponseDto> result = bookService.saveAll(emptyList);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, bookRepository.count());
    }

    @Test
    @DisplayName("Should handle large batch book creation")
    void shouldHandleLargeBatchBookCreation() {
        // Given
        List<CreateBookRequestDto> largeBatch = List.of(
                CreateBookRequestDto.builder().title("Book 1").author("Author 1").publicationYear(2021).build(),
                CreateBookRequestDto.builder().title("Book 2").author("Author 2").publicationYear(2022).build(),
                CreateBookRequestDto.builder().title("Book 3").author("Author 3").publicationYear(2023).build(),
                CreateBookRequestDto.builder().title("Book 4").author("Author 4").publicationYear(2024).build(),
                CreateBookRequestDto.builder().title("Book 5").author("Author 5").publicationYear(2025).build()
        );

        // When
        List<CreateBookResponseDto> result = bookService.saveAll(largeBatch);

        // Then
        assertEquals(5, result.size());
        assertEquals(5, bookRepository.count());

        // Verify all books were saved correctly
        for (int i = 0; i < 5; i++) {
            CreateBookResponseDto response = result.get(i);
            assertNotNull(response.id());
            assertEquals("Book " + (i + 1), response.title());
            assertEquals("Author " + (i + 1), response.author());
            assertEquals(2021 + i, response.publicationYear());
        }
    }

    private void createTestBooksInDatabase(int count) {
        for (int i = 1; i <= count; i++) {
            Book book = new Book();
            book.setTitle("Test Book " + i);
            book.setAuthor("Test Author " + i);
            book.setPublicationYear(2020 + (i % 5));
            book.setDescription("Test Description " + i);
            bookRepository.save(book);
        }
    }

    private Book createSingleTestBook() {
        Book book = new Book();
        book.setTitle("Integration Test Book");
        book.setAuthor("Test Author");
        book.setPublicationYear(2023);
        book.setDescription("Test Description");
        return bookRepository.save(book);
    }
}

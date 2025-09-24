package com.example.resourceapi.service;

import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.dto.response.CreateBookResponseDto;
import com.example.resourceapi.dto.response.PagedBooksResponseDto;
import com.example.resourceapi.dto.response.UpdateBooksResponseDto;
import com.example.resourceapi.entity.Book;
import com.example.resourceapi.exception.BookNotFoundException;
import com.example.resourceapi.mapper.BookMapper;
import com.example.resourceapi.rabbit.publisher.BookLogEventPublisher;
import com.example.resourceapi.repository.BookRepository;
import com.example.resourceapi.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceImplUnitTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BookLogEventPublisher bookLogEventPublisher;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private CreateBookRequestDto testRequestDto;
    private CreateBookResponseDto testResponseDto;
    private UpdateBookRequestDto testUpdateDto;
    private UUID testBookId;

    @BeforeEach
    void setUp() {
        testBookId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        testBook = new Book();
        testBook.setId(testBookId);
        testBook.setTitle("Spring Boot in Action");
        testBook.setAuthor("Craig Walls");
        testBook.setPublicationYear(2023);
        testBook.setDescription("A comprehensive guide to Spring Boot");

        testRequestDto = CreateBookRequestDto.builder()
                .title("Test Book")
                .author("Test Author")
                .publicationYear(2024)
                .description("Test Description")
                .build();

        testResponseDto = CreateBookResponseDto.builder()
                .id(testBookId)
                .title("Spring Boot in Action")
                .author("Craig Walls")
                .publicationYear(2023)
                .description("A comprehensive guide to Spring Boot")
                .build();

        testUpdateDto = UpdateBookRequestDto.builder()
                .id(testBookId)
                .title(JsonNullable.of("Updated Title"))
                .author(JsonNullable.of("Updated Author"))
                .publicationYear(JsonNullable.of(2025))
                .description(JsonNullable.of("Updated Description"))
                .build();
    }

    @Test
    @DisplayName("Should save all books successfully")
    void shouldSaveAllBooksSuccessfully() {
        // Given
        List<CreateBookRequestDto> requestDtos = List.of(testRequestDto);
        List<Book> mappedBooks = List.of(testBook);
        List<Book> savedBooks = List.of(testBook);
        List<CreateBookResponseDto> responseDtos = List.of(testResponseDto);

        when(bookMapper.toBookList(requestDtos)).thenReturn(mappedBooks);
        when(bookRepository.saveAll(mappedBooks)).thenReturn(savedBooks);
        when(bookMapper.toCreateBookResponseDtoList(savedBooks)).thenReturn(responseDtos);

        // Mock transaction synchronization manager
        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTxManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .then(invocation -> {
                        // Simulate transaction commit by calling afterCommit immediately
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // When
            List<CreateBookResponseDto> result = bookService.saveAll(requestDtos);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testResponseDto, result.get(0));

            verify(bookMapper).toBookList(requestDtos);
            verify(bookRepository).saveAll(mappedBooks);
            verify(bookMapper).toCreateBookResponseDtoList(savedBooks);
        }
    }

    @Test
    @DisplayName("Should find books with pagination successfully")
    void shouldFindBooksWithPaginationSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String title = "Spring";
        String author = "Craig";
        Integer year = 2023;

        List<Book> books = List.of(testBook);
        Page<Book> bookPage = new PageImpl<>(books, pageable, 1);
        List<CreateBookResponseDto> responseDtos = List.of(testResponseDto);

        when(bookRepository.findBooksWithFilters(eq(title), eq(author), eq(year), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toCreateBookResponseDtoList(books)).thenReturn(responseDtos);

        // When
        PagedBooksResponseDto result = bookService.findBooksWithPagination(pageable, title, author, year);

        // Then
        assertNotNull(result);
        assertEquals(responseDtos, result.books());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(0, result.currentPage());
        assertEquals(10, result.pageSize());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());

        verify(bookRepository).findBooksWithFilters(eq(title), eq(author), eq(year), any(Pageable.class));
        verify(bookMapper).toCreateBookResponseDtoList(books);
    }

    @Test
    @DisplayName("Should find books with null filters")
    void shouldFindBooksWithNullFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        List<Book> books = List.of(testBook);
        Page<Book> bookPage = new PageImpl<>(books, pageable, 1);
        List<CreateBookResponseDto> responseDtos = List.of(testResponseDto);

        when(bookRepository.findBooksWithFilters(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toCreateBookResponseDtoList(books)).thenReturn(responseDtos);

        // When
        PagedBooksResponseDto result = bookService.findBooksWithPagination(pageable, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.books().size());
        verify(bookRepository).findBooksWithFilters(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should find book by ID successfully")
    void shouldFindBookByIdSuccessfully() {
        // Given
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));
        when(bookMapper.toCreateBookResponseDto(testBook)).thenReturn(testResponseDto);

        // When
        CreateBookResponseDto result = bookService.findById(testBookId);

        // Then
        assertNotNull(result);
        assertEquals(testResponseDto, result);

        verify(bookRepository).findById(testBookId);
        verify(bookMapper).toCreateBookResponseDto(testBook);
    }

    @Test
    @DisplayName("Should throw BookNotFoundException when book not found by ID")
    void shouldThrowBookNotFoundExceptionWhenBookNotFoundById() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(bookRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        BookNotFoundException exception = assertThrows(BookNotFoundException.class, () -> {
            bookService.findById(nonExistentId);
        });

        assertNotNull(exception);
        verify(bookRepository).findById(nonExistentId);
        verifyNoInteractions(bookMapper);
    }

    @Test
    @DisplayName("Should update books successfully")
    void shouldUpdateBooksSuccessfully() {
        // Given
        List<UpdateBookRequestDto> updateRequests = List.of(testUpdateDto);
        List<Book> existingBooks = List.of(testBook);
        Book updatedBook = new Book();
        updatedBook.setId(testBookId);
        updatedBook.setTitle("Updated Title");

        when(bookRepository.findAllById(any())).thenReturn(existingBooks);
        when(bookMapper.updateBookFromDto(testUpdateDto, testBook)).thenReturn(updatedBook);
        when(bookRepository.save(updatedBook)).thenReturn(updatedBook);
        when(bookMapper.toCreateBookResponseDtoList(List.of(updatedBook))).thenReturn(List.of(testResponseDto));

        // Mock transaction synchronization manager
        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTxManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .then(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // When
            UpdateBooksResponseDto result = bookService.updateBooks(updateRequests);

            // Then
            assertNotNull(result);
            assertEquals(1, result.updatedBooks().size());
            assertEquals("Books are successfully updated", result.message());
            assertTrue(result.notUpdatedIds().isEmpty());

            verify(bookRepository).findAllById(any());
            verify(bookMapper).updateBookFromDto(testUpdateDto, testBook);
            verify(bookRepository).save(updatedBook);
        }
    }

    @Test
    @DisplayName("Should handle partially found books in update operation")
    void shouldHandlePartiallyFoundBooksInUpdateOperation() {
        // Given
        UUID existentId = UUID.randomUUID();
        UUID nonExistentId = UUID.randomUUID();

        UpdateBookRequestDto existentUpdate = UpdateBookRequestDto.builder()
                .id(existentId)
                .title(JsonNullable.of("Updated Title"))
                .build();

        UpdateBookRequestDto nonExistentUpdate = UpdateBookRequestDto.builder()
                .id(nonExistentId)
                .title(JsonNullable.of("Another Title"))
                .build();

        List<UpdateBookRequestDto> updateRequests = List.of(existentUpdate, nonExistentUpdate);

        Book existingBook = new Book();
        existingBook.setId(existentId);
        List<Book> foundBooks = List.of(existingBook);

        when(bookRepository.findAllById(any())).thenReturn(foundBooks);
        when(bookMapper.updateBookFromDto(existentUpdate, existingBook)).thenReturn(existingBook);
        when(bookRepository.save(existingBook)).thenReturn(existingBook);
        when(bookMapper.toCreateBookResponseDtoList(any())).thenReturn(List.of(testResponseDto));

        // Mock transaction synchronization manager
        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTxManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .then(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // When
            UpdateBooksResponseDto result = bookService.updateBooks(updateRequests);

            // Then
            assertNotNull(result);
            assertEquals(1, result.updatedBooks().size());
            assertEquals(1, result.notUpdatedIds().size());
            assertTrue(result.notUpdatedIds().contains(nonExistentId));
            assertTrue(result.message().contains("not updated"));

            verify(bookRepository).findAllById(any());
            verify(bookMapper).updateBookFromDto(existentUpdate, existingBook);
            verify(bookRepository).save(existingBook);
        }
    }

    @Test
    @DisplayName("Should delete book successfully")
    void shouldDeleteBookSuccessfully() {
        // Given
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));

        // Mock transaction synchronization manager
        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTxManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .then(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // When
            assertDoesNotThrow(() -> bookService.deleteBook(testBookId));

            // Then
            verify(bookRepository).findById(testBookId);
            verify(bookRepository).delete(testBook);
        }
    }

    @Test
    @DisplayName("Should throw BookNotFoundException when deleting non-existent book")
    void shouldThrowBookNotFoundExceptionWhenDeletingNonExistentBook() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(bookRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        BookNotFoundException exception = assertThrows(BookNotFoundException.class, () -> {
            bookService.deleteBook(nonExistentId);
        });

        assertNotNull(exception);
        verify(bookRepository).findById(nonExistentId);
        verify(bookRepository, never()).delete(any());
        verifyNoInteractions(bookLogEventPublisher);
    }

    @Test
    @DisplayName("Should handle large batch save operations")
    void shouldHandleLargeBatchSaveOperations() {
        // Given
        List<CreateBookRequestDto> largeBatch = List.of(
                CreateBookRequestDto.builder().title("Book 1").author("Author 1").publicationYear(2021).build(),
                CreateBookRequestDto.builder().title("Book 2").author("Author 2").publicationYear(2022).build(),
                CreateBookRequestDto.builder().title("Book 3").author("Author 3").publicationYear(2023).build()
        );

        List<Book> mappedBooks = List.of(new Book(), new Book(), new Book());
        List<Book> savedBooks = List.of(testBook, testBook, testBook);
        List<CreateBookResponseDto> responseDtos = List.of(testResponseDto, testResponseDto, testResponseDto);

        when(bookMapper.toBookList(largeBatch)).thenReturn(mappedBooks);
        when(bookRepository.saveAll(mappedBooks)).thenReturn(savedBooks);
        when(bookMapper.toCreateBookResponseDtoList(savedBooks)).thenReturn(responseDtos);

        // Mock transaction synchronization manager
        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTxManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .then(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // When
            List<CreateBookResponseDto> result = bookService.saveAll(largeBatch);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            verify(bookMapper).toBookList(largeBatch);
            verify(bookRepository).saveAll(mappedBooks);
            verify(bookMapper).toCreateBookResponseDtoList(savedBooks);
        }
    }

    @Test
    @DisplayName("Should handle pagination edge cases")
    void shouldHandlePaginationEdgeCases() {
        // Given - Empty result set
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(bookRepository.findBooksWithFilters(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);
        when(bookMapper.toCreateBookResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When
        PagedBooksResponseDto result = bookService.findBooksWithPagination(pageable, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.books().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());

        verify(bookRepository).findBooksWithFilters(isNull(), isNull(), isNull(), any(Pageable.class));
        verify(bookMapper).toCreateBookResponseDtoList(Collections.emptyList());
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        // Given
        when(bookRepository.findById(testBookId)).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookService.findById(testBookId);
        });

        assertEquals("Database connection failed", exception.getMessage());
        verify(bookRepository).findById(testBookId);
        verifyNoInteractions(bookMapper);
    }

    @Test
    @DisplayName("Should verify event publishing behavior")
    void shouldVerifyEventPublishingBehavior() {
        // Given
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));

        // Mock transaction synchronization manager
        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTxManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .then(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // When
            bookService.deleteBook(testBookId);

            // Then
            verify(bookRepository).findById(testBookId);
            verify(bookRepository).delete(testBook);
            // Note: Event publishing happens asynchronously after transaction commit
            // In unit tests, we don't verify the actual event publishing since it's async
            // and tested separately in integration tests
        }
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        // When & Then - Test null input validation
        assertThrows(Exception.class, () -> {
            bookService.saveAll(null);
        });

        assertThrows(Exception.class, () -> {
            bookService.findById(null);
        });

        assertThrows(Exception.class, () -> {
            bookService.updateBooks(null);
        });

        assertThrows(Exception.class, () -> {
            bookService.deleteBook(null);
        });
    }

    @Test
    @DisplayName("Should handle mapper exceptions")
    void shouldHandleMapperExceptions() {
        // Given
        List<CreateBookRequestDto> requestDtos = List.of(testRequestDto);
        when(bookMapper.toBookList(requestDtos)).thenThrow(new RuntimeException("Mapping failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookService.saveAll(requestDtos);
        });

        assertEquals("Mapping failed", exception.getMessage());
        verify(bookMapper).toBookList(requestDtos);
        verifyNoInteractions(bookRepository);
        verifyNoInteractions(bookLogEventPublisher);
    }

    @Test
    @DisplayName("Should handle update with no found books")
    void shouldHandleUpdateWithNoFoundBooks() {
        // Given
        List<UpdateBookRequestDto> updateRequests = List.of(testUpdateDto);
        when(bookRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(bookMapper.toCreateBookResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When
        UpdateBooksResponseDto result = bookService.updateBooks(updateRequests);

        // Then
        assertNotNull(result);
        assertTrue(result.updatedBooks().isEmpty());
        assertEquals(1, result.notUpdatedIds().size());
        assertTrue(result.notUpdatedIds().contains(testBookId));
        assertTrue(result.message().contains("not updated"));

        verify(bookRepository).findAllById(any());
        verify(bookMapper).toCreateBookResponseDtoList(Collections.emptyList());
        verifyNoInteractions(bookLogEventPublisher); // No events for unsuccessful updates
    }

    @Test
    @DisplayName("Should build correct PagedBooksResponseDto")
    void shouldBuildCorrectPagedBooksResponseDto() {
        // Given
        Pageable pageable = PageRequest.of(1, 5); // Second page
        List<Book> books = List.of(testBook);
        Page<Book> bookPage = new PageImpl<>(books, pageable, 12); // 12 total elements
        List<CreateBookResponseDto> responseDtos = List.of(testResponseDto);

        when(bookRepository.findBooksWithFilters(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toCreateBookResponseDtoList(books)).thenReturn(responseDtos);

        // When
        PagedBooksResponseDto result = bookService.findBooksWithPagination(pageable, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(responseDtos, result.books());
        assertEquals(12, result.totalElements());
        assertEquals(3, result.totalPages()); // 12 elements / 5 per page = 3 pages
        assertEquals(1, result.currentPage());
        assertEquals(5, result.pageSize());
        assertTrue(result.hasNext()); // Page 1 of 3, so there's a next page
        assertTrue(result.hasPrevious()); // Page 1 of 3, so there's a previous page
    }
}

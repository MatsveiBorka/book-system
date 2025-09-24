package com.example.resourceapi.service.impl;

import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.response.CreateBookResponseDto;
import com.example.resourceapi.dto.response.PagedBooksResponseDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.dto.response.UpdateBooksResponseDto;
import com.example.resourceapi.enums.EventType;
import com.example.resourceapi.exception.BookNotFoundException;
import com.example.resourceapi.entity.Book;
import com.example.resourceapi.mapper.BookMapper;
import com.example.resourceapi.rabbit.event.BookLogEvent;
import com.example.resourceapi.rabbit.publisher.BookLogEventPublisher;
import com.example.resourceapi.repository.BookRepository;
import com.example.resourceapi.repository.util.PageableUtil;
import com.example.resourceapi.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookLogEventPublisher bookLogEventPublisher;

    private final static String EVENT_SUBJECT_TYPE = "Book";

    @Override
    @Transactional
    public List<CreateBookResponseDto> saveAll(List<CreateBookRequestDto> books) {
        List<Book> mappedBooks = bookMapper.toBookList(books);
        List<Book> savedBooks = bookRepository.saveAll(mappedBooks);

        publishBookLogEvent(EventType.CREATE, savedBooks);
        return bookMapper.toCreateBookResponseDtoList(savedBooks);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedBooksResponseDto findBooksWithPagination(
            Pageable pageable,
            String title,
            String author,
            Integer publicationYear) {

        Pageable sanitizePageable = PageableUtil.sanitizePageable(pageable);

        Page<Book> bookPage = bookRepository.findBooksWithFilters(title, author, publicationYear, sanitizePageable);

        List<CreateBookResponseDto> books = bookMapper.toCreateBookResponseDtoList(bookPage.getContent());

        return PagedBooksResponseDto.builder()
                .books(books)
                .totalElements(bookPage.getTotalElements())
                .totalPages(bookPage.getTotalPages())
                .currentPage(bookPage.getNumber())
                .pageSize(bookPage.getSize())
                .hasNext(bookPage.hasNext())
                .hasPrevious(bookPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CreateBookResponseDto findById(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        return bookMapper.toCreateBookResponseDto(book);
    }

    @Override
    @Transactional
    public UpdateBooksResponseDto updateBooks(List<UpdateBookRequestDto> updateRequests) {
        Map<UUID, UpdateBookRequestDto> dtoMap = updateRequests.stream()
                .collect(Collectors.toMap(UpdateBookRequestDto::id, Function.identity()));

        List<Book> existingBooks = bookRepository.findAllById(dtoMap.keySet());

        // Determine which books were found and which were not
        Set<UUID> foundIds = existingBooks.stream().map(Book::getId).collect(Collectors.toSet());
        List<UUID> notFoundIds = dtoMap.keySet().stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        // Update only the books that were found
        List<Book> updatedBooks = existingBooks.stream()
                .map(book -> {
                    UpdateBookRequestDto dto = dtoMap.get(book.getId());
                    return bookMapper.updateBookFromDto(dto, book);
                })
                .map(bookRepository::save)
                .toList();

        // Publish events only for successfully updated books
        if (!updatedBooks.isEmpty()) {
            publishBookLogEvent(EventType.UPDATE, updatedBooks);
        }

        // Generate appropriate message
        String message;
        if (notFoundIds.isEmpty()) {
            message = "Books are successfully updated";
        } else {
            message = "Books with IDs " + notFoundIds + " are not updated";
        }

        return UpdateBooksResponseDto.builder()
                .updatedBooks(bookMapper.toCreateBookResponseDtoList(updatedBooks))
                .message(message)
                .notUpdatedIds(notFoundIds)
                .build();
    }

    @Override
    @Transactional
    public void deleteBook(UUID id) {
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        bookRepository.delete(existingBook);

        publishBookLogEvent(EventType.DELETE, List.of(existingBook));
    }

    private void publishBookLogEvent(EventType update, List<Book> updatedBook) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        BookLogEvent bookLogEvent = createBookLogEvent(update, updatedBook);
                        bookLogEventPublisher.publishEvent(bookLogEvent);
                    }
                }
        );
    }

    private BookLogEvent createBookLogEvent(EventType eventType, List<Book> books) {
        String bookIds = books.stream()
                .map(Book::getId)
                .map(UUID::toString)
                .collect(Collectors.joining(", "));

        return BookLogEvent.builder()
                .timestamp(Instant.now())
                .subjectType(EVENT_SUBJECT_TYPE)
                .eventType(eventType)
                .eventDescription(
                        eventType == EventType.CREATE ? "New books were created with IDs: " + bookIds :
                                eventType == EventType.UPDATE ? "Books were updated with IDs: " + bookIds :
                                        eventType == EventType.DELETE ? "Books were deleted with IDs " + bookIds : "Unknown event. ")
                .build();
    }
}

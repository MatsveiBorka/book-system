package com.example.resourceapi.service;

import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.dto.response.CreateBookResponseDto;
import com.example.resourceapi.dto.response.PagedBooksResponseDto;
import com.example.resourceapi.dto.response.UpdateBooksResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BookService {
    List<CreateBookResponseDto> saveAll(List<CreateBookRequestDto> books);

    PagedBooksResponseDto findBooksWithPagination(
            Pageable pageable,
            String title,
            String author,
            Integer publicationYear);

    CreateBookResponseDto findById(UUID id);

    UpdateBooksResponseDto updateBooks(List<UpdateBookRequestDto> updateRequests);


    void deleteBook(UUID id);
}

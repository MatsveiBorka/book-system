package com.example.resourceapi.mapper;

import com.example.resourceapi.dto.request.CreateBookRequestDto;
import com.example.resourceapi.dto.response.CreateBookResponseDto;
import com.example.resourceapi.dto.request.UpdateBookRequestDto;
import com.example.resourceapi.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {

    CreateBookResponseDto toCreateBookResponseDto(Book book);

    @Mapping(target = "id", ignore = true)
    Book toBook(CreateBookRequestDto createBookRequestDto);

    List<CreateBookResponseDto> toCreateBookResponseDtoList(List<Book> books);
    List<Book> toBookList(List<CreateBookRequestDto> books);

    default Book updateBookFromDto(UpdateBookRequestDto updateRequest, Book existingBook) {
        if (updateRequest == null || existingBook == null) {
            return existingBook;
        }

        if (updateRequest.title() != null && updateRequest.title().isPresent()) {
            existingBook.setTitle(updateRequest.title().get());
        }

        if (updateRequest.author() != null && updateRequest.author().isPresent()) {
            existingBook.setAuthor(updateRequest.author().get());
        }

        if (updateRequest.publicationYear() != null && updateRequest.publicationYear().isPresent()) {
            existingBook.setPublicationYear(updateRequest.publicationYear().get());
        }

        if (updateRequest.description() != null && updateRequest.description().isPresent()) {
            existingBook.setDescription(updateRequest.description().get());
        }

        return existingBook;
    }
}

package com.example.resourceapi.repository;

import com.example.resourceapi.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    @Query("SELECT b FROM Book b WHERE " +
            "(:title IS NULL OR (:title = '' AND b.title = '') OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:author IS NULL OR (:author = '' AND b.author = '') OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
            "(:publicationYear IS NULL OR b.publicationYear = :publicationYear)")
    Page<Book> findBooksWithFilters(@Param("title") String title,
                                    @Param("author") String author,
                                    @Param("publicationYear") Integer publicationYear,
                                    Pageable pageable);
}


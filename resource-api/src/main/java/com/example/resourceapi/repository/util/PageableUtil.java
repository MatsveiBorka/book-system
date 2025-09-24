package com.example.resourceapi.repository.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PageableUtil {

    public static Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? 10 : pageable.getPageSize();

        Sort sort = pageable.getSort();
        Set<String> allowedFields = Set.of("title", "author", "publicationYear");

        List<Sort.Order> sanitizedOrders = new ArrayList<>();
        for (Sort.Order order : sort) {
            try {
                if (allowedFields.contains(order.getProperty())) {
                    Sort.Direction direction = order.getDirection(); // ASC/DESC only
                    sanitizedOrders.add(new Sort.Order(direction, order.getProperty()));
                }
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }

        Sort sanitizedSort = sanitizedOrders.isEmpty() ? Sort.by("title").ascending() : Sort.by(sanitizedOrders);

        return PageRequest.of(page, size, sanitizedSort);
    }
}

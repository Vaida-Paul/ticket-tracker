package com.example.skeleton.utils;

import com.example.skeleton.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

public final class RequestValidationUtils {

    private RequestValidationUtils() {}

    public static Pageable buildPageable(
        Integer page,
        Integer size,
        String sortBy,
        String defaultSortBy,
        String sortDirection,
        Set<String> allowedSortFields
    ) {
        return buildPageable(page, size, sortBy, defaultSortBy, sortDirection, allowedSortFields, Map.of());
    }

    public static Pageable buildPageable(
            Integer page,
            Integer size,
            String sortBy,
            String defaultSortBy,
            String sortDirection,
            Set<String> allowedSortFields,
            Map<String, String> sortAliases
    ) {
        String sortField = parseSortField(sortBy, defaultSortBy, allowedSortFields);
        String actualField = sortAliases.getOrDefault(sortField, sortField);
        Sort.Direction direction = parseSortDirection(sortDirection);
        return PageRequest.of(parsePage(page), parseSize(size), Sort.by(direction, actualField));
    }

    public static Sort.Direction parseSortDirection(String value) {
        String v = (value == null || value.isBlank()) ? "desc" : value;
        try {
            return Sort.Direction.fromString(v);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(
                    "Invalid sortDirection: '" + v + "'. Allowed values: asc, desc");
        }
    }

    public static int parsePage(Integer page) {
        if (page == null) return 0;
        if (page < 0) throw new InvalidRequestException("page must be >= 0");
        return page;
    }

    public static int parseSize(Integer size) {
        if (size == null) return 20;
        if (size < 1 || size > 100) throw new InvalidRequestException("size must be between 1 and 100");
        return size;
    }

    public static String parseSortField(String value, String defaultValue, Set<String> allowed) {
        String v = (value == null || value.isBlank()) ? defaultValue : value;
        if (!allowed.contains(v)) {
            throw new InvalidRequestException(
                    "Invalid sortBy: '" + v + "'. Allowed values: " + allowed);
        }
        return v;
    }
}
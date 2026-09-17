package com.example.skeleton.mapper;

import org.mapstruct.Condition;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(componentModel = "spring")
public interface JsonNullableMapper {

    @Condition
    default <T> boolean isPresent(JsonNullable<T> jsonNullable) {
        return jsonNullable != null && jsonNullable.isPresent();
    }

    @Named("unwrap")
    default <T> T unwrap(JsonNullable<T> jsonNullable) {
        return jsonNullable == null ? null : jsonNullable.orElse(null);
    }
}
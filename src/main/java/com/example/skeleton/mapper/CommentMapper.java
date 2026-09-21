package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.model.CommentCreateRequest;
import org.openapitools.model.CommentDto;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "ticket.id", target = "ticketId")
    CommentDto toDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Comment toEntity(CommentCreateRequest request);
}
package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.Ticket;
import org.mapstruct.*;
import org.openapitools.model.*;

@Mapper(
        componentModel = "spring",
        uses = {EnumMapper.class, JsonNullableMapper.class}
)
public interface TicketMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "assignee.id", target = "assigneeId")
    TicketDto toDto(Ticket ticket);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    Ticket toEntity(TicketCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    void replaceFromRequest(TicketPutRequest request, @MappingTarget Ticket entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "description", target = "description", qualifiedByName = "unwrap")
    @Mapping(source = "dueDate", target = "dueDate", qualifiedByName = "unwrap")
    void patchFromRequest(TicketPatchRequest request, @MappingTarget Ticket entity);
}
package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.Ticket;
import org.mapstruct.*;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.model.*;

@Mapper(
        componentModel = "spring",
        uses = EnumMapper.class
)
public interface TicketMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "assignee.id", target = "assigneeId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "priority", target = "priority")
    TicketDto toDto(Ticket ticket);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "status", target = "status")
    @Mapping(source = "priority", target = "priority")
    Ticket toEntity(TicketCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "status", target = "status")
    @Mapping(source = "priority", target = "priority")
    void replaceFromRequest(TicketPutRequest request, @MappingTarget Ticket entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "dueDate", ignore = true)
    @Mapping(source = "status", target = "status")
    @Mapping(source = "priority", target = "priority")
    void patchFromRequest(TicketPatchRequest request, @MappingTarget Ticket entity);
}
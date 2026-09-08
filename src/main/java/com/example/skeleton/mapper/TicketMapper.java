package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.model.TicketCreateRequest;
import org.openapitools.model.TicketDto;
import org.openapitools.model.TicketUpdateRequest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TicketMapper {

    default OffsetDateTime map(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC);
    }

    default LocalDateTime map(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime();
    }

    @Mapping(source = "project.id", target = "project.id")
    @Mapping(source = "project.name", target = "project.name")
    @Mapping(source = "project.description", target = "project.description")
    @Mapping(source = "project.createdBy", target = "project.createdBy")
    @Mapping(source = "project.createdAt", target = "project.createdAt")
    @Mapping(source = "assignee.id", target = "assignee.id")
    @Mapping(source = "assignee.email", target = "assignee.email")
    @Mapping(source = "assignee.fullName", target = "assignee.fullName")
    @Mapping(source = "dueDate", target = "dueDate")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    TicketDto toDto(Ticket ticket);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "dueDate", target = "dueDate")
    Ticket toEntity(TicketCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "dueDate", target = "dueDate")
    void updateFromRequest(TicketUpdateRequest request, @MappingTarget Ticket entity);
}
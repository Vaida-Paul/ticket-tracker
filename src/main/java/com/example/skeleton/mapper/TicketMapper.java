package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.Ticket;
import org.mapstruct.*;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.model.*;

@Mapper(
        componentModel = "spring",
        uses = EnumMapper.class
)
public interface TicketMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "assignee.id", target = "assigneeId")
    @Mapping(source = "status", target = "status", qualifiedByName = "domainToOpenApiTicketStatus")
    @Mapping(source = "priority", target = "priority", qualifiedByName = "domainToOpenApiPriority")
    TicketDto toDto(Ticket ticket);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "status", target = "status", qualifiedByName = "openApiToDomainTicketStatus")
    @Mapping(source = "priority", target = "priority", qualifiedByName = "openApiToDomainPriority")
    Ticket toEntity(TicketCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "status", target = "status", qualifiedByName = "openApiToDomainTicketStatus")
    @Mapping(source = "priority", target = "priority", qualifiedByName = "openApiToDomainPriority")
    void replaceFromRequest(TicketPutRequest request, @MappingTarget Ticket entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(source = "status", target = "status", qualifiedByName = "openApiToDomainTicketStatus")
    @Mapping(source = "priority", target = "priority", qualifiedByName = "openApiToDomainPriority")
    void patchFromRequest(TicketUpdateRequest request, @MappingTarget Ticket entity);
}
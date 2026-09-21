package com.example.skeleton.mapper;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnumMapper {

    com.example.skeleton.domain.enums.TicketStatus toDomainTicketStatus(
            org.openapitools.model.TicketStatus status);

    com.example.skeleton.domain.enums.Priority toDomainPriority(
            org.openapitools.model.Priority priority);

    org.openapitools.model.TicketStatus toOpenApiTicketStatus(
            com.example.skeleton.domain.enums.TicketStatus status);

    org.openapitools.model.Priority toOpenApiPriority(
            com.example.skeleton.domain.enums.Priority priority);
}
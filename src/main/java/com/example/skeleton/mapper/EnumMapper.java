package com.example.skeleton.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface EnumMapper {

    @Named("openApiToDomainTicketStatus")
    default com.example.skeleton.domain.enums.TicketStatus toDomainTicketStatus(
            org.openapitools.model.TicketStatus status) {
        if (status == null) return null;
        return switch (status) {
            case OPEN -> com.example.skeleton.domain.enums.TicketStatus.OPEN;
            case IN_PROGRESS -> com.example.skeleton.domain.enums.TicketStatus.IN_PROGRESS;
            case DONE -> com.example.skeleton.domain.enums.TicketStatus.DONE;
            case CANCELLED -> com.example.skeleton.domain.enums.TicketStatus.CANCELLED;
        };
    }

    @Named("openApiToDomainPriority")
    default com.example.skeleton.domain.enums.Priority toDomainPriority(
            org.openapitools.model.Priority priority) {
        if (priority == null) return null;
        return switch (priority) {
            case LOW -> com.example.skeleton.domain.enums.Priority.LOW;
            case MEDIUM -> com.example.skeleton.domain.enums.Priority.MEDIUM;
            case HIGH -> com.example.skeleton.domain.enums.Priority.HIGH;
            case URGENT -> com.example.skeleton.domain.enums.Priority.URGENT;
        };
    }

    @Named("domainToOpenApiTicketStatus")
    default org.openapitools.model.TicketStatus toOpenApiTicketStatus(
            com.example.skeleton.domain.enums.TicketStatus status) {
        if (status == null) return null;
        return switch (status) {
            case OPEN -> org.openapitools.model.TicketStatus.OPEN;
            case IN_PROGRESS -> org.openapitools.model.TicketStatus.IN_PROGRESS;
            case DONE -> org.openapitools.model.TicketStatus.DONE;
            case CANCELLED -> org.openapitools.model.TicketStatus.CANCELLED;
        };
    }

    @Named("domainToOpenApiPriority")
    default org.openapitools.model.Priority toOpenApiPriority(
            com.example.skeleton.domain.enums.Priority priority) {
        if (priority == null) return null;
        return switch (priority) {
            case LOW -> org.openapitools.model.Priority.LOW;
            case MEDIUM -> org.openapitools.model.Priority.MEDIUM;
            case HIGH -> org.openapitools.model.Priority.HIGH;
            case URGENT -> org.openapitools.model.Priority.URGENT;
        };
    }
}
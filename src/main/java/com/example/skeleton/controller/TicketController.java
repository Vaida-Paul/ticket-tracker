package com.example.skeleton.controller;

import com.example.skeleton.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.TicketsApi;
import org.openapitools.model.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class TicketController implements TicketsApi {

    private final TicketService ticketService;

    @Override
    public ResponseEntity<PageResponseTicketDto> getTickets(
            TicketStatus status,
            Priority priority,
            Long assigneeId,
            LocalDate dueBefore,
            LocalDate dueAfter,
            String titleContains,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        Page<TicketDto> ticketPage = ticketService.searchTickets(
                status, priority, assigneeId, dueBefore, dueAfter,
                titleContains, page, size, sortBy, sortDirection
        );

        PageResponseTicketDto response = new PageResponseTicketDto();
        response.setContent(ticketPage.getContent());
        response.setTotalElements(ticketPage.getTotalElements());
        response.setTotalPages(ticketPage.getTotalPages());
        response.setSize(ticketPage.getSize());
        response.setPage(ticketPage.getNumber());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TicketDto> getTicketById(Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    @Override
    public ResponseEntity<TicketDto> createTicket(@Valid TicketCreateRequest request) {
        TicketDto ticket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @Override
    public ResponseEntity<TicketDto> updateTicket(Long ticketId, @Valid TicketPutRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(ticketId, request));
    }

    @Override
    public ResponseEntity<TicketDto> patchTicket(Long ticketId, @Valid TicketPatchRequest request) {
        return ResponseEntity.ok(ticketService.partialUpdateTicket(ticketId, request));
    }

    @Override
    public ResponseEntity<Void> deleteTicket(Long ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.noContent().build();
    }
}
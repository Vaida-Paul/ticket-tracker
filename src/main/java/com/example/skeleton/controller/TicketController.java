package com.example.skeleton.controller;

import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.mapper.TicketMapper;
import com.example.skeleton.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.TicketsApi;
import org.openapitools.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final TicketMapper ticketMapper;

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
        String sortField = sortBy != null && !sortBy.isEmpty() ? sortBy : "priority";
        Sort.Direction direction = Sort.Direction.fromString(
                sortDirection != null && !sortDirection.isEmpty() ? sortDirection : "desc"
        );
        Sort sortObj = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                sortObj
        );

        TicketSearchRequest searchRequest = new TicketSearchRequest();
        searchRequest.setStatus(status);
        searchRequest.setPriority(priority);
        searchRequest.setAssigneeId(assigneeId);
        searchRequest.setDueBefore(dueBefore);
        searchRequest.setDueAfter(dueAfter);
        searchRequest.setTitleContains(titleContains);

        Page<Ticket> ticketPage = ticketService.searchTickets(searchRequest, pageable);

        PageResponseTicketDto response = new PageResponseTicketDto();
        response.setContent(ticketPage.getContent().stream()
                .map(ticketMapper::toDto)
                .toList());
        response.setTotalElements(ticketPage.getTotalElements());
        response.setTotalPages(ticketPage.getTotalPages());
        response.setSize(ticketPage.getSize());
        response.setPage(ticketPage.getNumber());

        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<TicketDto> getTicketById(Long ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        return ResponseEntity.ok(ticketMapper.toDto(ticket));
    }

    @Override
    public ResponseEntity<TicketDto> createTicket(@Valid TicketCreateRequest request) {
        Ticket ticket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketMapper.toDto(ticket));
    }

    @Override
    public ResponseEntity<TicketDto> updateTicket(Long ticketId,@Valid TicketUpdateRequest request) {
        Ticket ticket = ticketService.updateTicket(ticketId, request);
        return ResponseEntity.ok(ticketMapper.toDto(ticket));
    }

    @Override
    public ResponseEntity<TicketDto> patchTicket(Long ticketId, TicketUpdateRequest request) {
        Ticket ticket = ticketService.partialUpdateTicket(ticketId, request);
        return ResponseEntity.ok(ticketMapper.toDto(ticket));
    }

    @Override
    public ResponseEntity<Void> deleteTicket(Long ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.noContent().build();
    }
}

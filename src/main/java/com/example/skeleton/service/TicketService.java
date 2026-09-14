package com.example.skeleton.service;

import com.example.skeleton.domain.model.Ticket;
import org.openapitools.model.TicketCreateRequest;
import org.openapitools.model.TicketSearchRequest;
import org.openapitools.model.TicketUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {

    Page<Ticket> searchTickets(TicketSearchRequest searchRequest, Pageable pageable);

    Ticket getTicketById(Long id);

    Ticket createTicket(TicketCreateRequest request);

    Ticket updateTicket(Long id, TicketUpdateRequest request);

    Ticket partialUpdateTicket(Long id, TicketUpdateRequest request);

    void deleteTicket(Long id);
}
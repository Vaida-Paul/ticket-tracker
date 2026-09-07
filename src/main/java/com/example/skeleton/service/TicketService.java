package com.example.skeleton.service;

import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.dto.TicketSearchRequest;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.specification.TicketSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public Page<Ticket> searchTickets(TicketSearchRequest searchRequest, Pageable pageable) {
        TicketSpecificationBuilder builder = new TicketSpecificationBuilder();

        Specification<Ticket> spec = builder
                .withStatus(searchRequest.getStatus())
                .withPriority(searchRequest.getPriority())
                .withAssignee(searchRequest.getAssigneeId())
                .withDueBefore(searchRequest.getDueBefore())
                .withDueAfter(searchRequest.getDueAfter())
                .withTitleContains(searchRequest.getTitleContains())
                .build();

        return ticketRepository.findAll(spec, pageable);
    }
}
package com.example.skeleton.service;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.mapper.EnumMapper;
import com.example.skeleton.mapper.TicketMapper;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.specification.TicketSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.TicketCreateRequest;
import org.openapitools.model.TicketSearchRequest;
import org.openapitools.model.TicketUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TicketMapper ticketMapper;
    private final EnumMapper enumMapper;

    @Transactional(readOnly = true)
    public Page<Ticket> searchTickets(TicketSearchRequest searchRequest, Pageable pageable) {
        TicketSpecificationBuilder builder = new TicketSpecificationBuilder();

        Specification<Ticket> spec = builder
                .withStatus(enumMapper.toDomainTicketStatus(searchRequest.getStatus()))
                .withPriority(enumMapper.toDomainPriority(searchRequest.getPriority()))
                .withAssignee(searchRequest.getAssigneeId())
                .withDueBefore(searchRequest.getDueBefore())
                .withDueAfter(searchRequest.getDueAfter())
                .withTitleContains(searchRequest.getTitleContains())
                .build();

        return ticketRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public Ticket createTicket(TicketCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new UserNotFoundException(request.getAssigneeId()));
        }

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setProject(project);
        ticket.setAssignee(assignee);
        if (ticket.getStatus() == null) {
            ticket.setStatus(com.example.skeleton.domain.enums.TicketStatus.OPEN);
        }

        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicket(Long id, TicketUpdateRequest request) {
        Ticket ticket = getTicketById(id);

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new UserNotFoundException(request.getAssigneeId()));
            ticket.setAssignee(assignee);
        }

        ticketMapper.updateFromRequest(request, ticket);

        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket partialUpdateTicket(Long id, TicketUpdateRequest request) {
        return updateTicket(id, request);
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = getTicketById(id);
        ticketRepository.delete(ticket);
    }
}
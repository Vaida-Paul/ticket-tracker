package com.example.skeleton.service.implementation;

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
import com.example.skeleton.service.TicketService;
import com.example.skeleton.specification.TicketSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.TicketCreateRequest;
import org.openapitools.model.TicketSearchRequest;
import org.openapitools.model.TicketUpdateRequest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class TicketServiceImp implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TicketMapper ticketMapper;
    private final EnumMapper enumMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> searchTickets(TicketSearchRequest searchRequest, Pageable pageable) {
        boolean sortByPriority = pageable.getSort().stream()
                .anyMatch(o -> "priority".equals(o.getProperty()));

        if (sortByPriority) {
            return searchWithPrioritySort(searchRequest, pageable);
        }

        return ticketRepository.findAll(buildSpecification(searchRequest), pageable);
    }

    private Page<Ticket> searchWithPrioritySort(TicketSearchRequest searchRequest, Pageable pageable) {
        Sort.Order priorityOrder = pageable.getSort().stream()
                .filter(o -> "priority".equals(o.getProperty()))
                .findFirst()
                .orElse(Sort.Order.desc("priority"));

        boolean ascending = priorityOrder.isAscending();

        List<Ticket> allTickets = new ArrayList<>(
                ticketRepository.findAll(buildSpecification(searchRequest)));

        Comparator<Ticket> comparator = Comparator.comparing(
                Ticket::getPriority,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        if (!ascending) {
            comparator = comparator.reversed();
        }

        allTickets.sort(comparator);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTickets.size());
        List<Ticket> pageContent = start <= allTickets.size()
                ? allTickets.subList(start, Math.min(end, allTickets.size()))
                : List.of();

        return new PageImpl<>(pageContent, pageable, allTickets.size());
    }

    private Specification<Ticket> buildSpecification(TicketSearchRequest searchRequest) {
        return new TicketSpecificationBuilder()
                .withStatus(enumMapper.toDomainTicketStatus(searchRequest.getStatus()))
                .withPriority(enumMapper.toDomainPriority(searchRequest.getPriority()))
                .withAssignee(searchRequest.getAssigneeId())
                .withDueBefore(searchRequest.getDueBefore())
                .withDueAfter(searchRequest.getDueAfter())
                .withTitleContains(searchRequest.getTitleContains())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Override
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

    @Override
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

    @Override
    @Transactional
    public Ticket partialUpdateTicket(Long id, TicketUpdateRequest request) {
        return updateTicket(id, request);
    }

    @Override
    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = getTicketById(id);
        ticketRepository.delete(ticket);
    }
}
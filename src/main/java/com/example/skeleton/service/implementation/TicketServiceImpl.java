package com.example.skeleton.service.implementation;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.InvalidRequestException;
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
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.example.skeleton.utils.RequestValidationUtils.*;

@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TicketMapper ticketMapper;
    private final EnumMapper enumMapper;

    private static final Set<String> SORT_FIELDS =
            Set.of("priority", "dueDate", "createdAt", "updatedAt", "title", "status");

    @Override
    @Transactional(readOnly = true)
    public Page<TicketDto> searchTickets(
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
        if (dueBefore != null && dueAfter != null && dueAfter.isAfter(dueBefore)) {
            throw new InvalidRequestException("dueAfter must be before or equal to dueBefore");
        }
        String sortField = parseSortField(sortBy, "priority", SORT_FIELDS);
        Sort.Direction direction = parseSortDirection(sortDirection);
        Sort sortObj = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(parsePage(page), parseSize(size), sortObj);

        TicketSearchRequest searchRequest = new TicketSearchRequest();
        searchRequest.setStatus(status);
        searchRequest.setPriority(priority);
        searchRequest.setAssigneeId(assigneeId);
        searchRequest.setDueBefore(dueBefore);
        searchRequest.setDueAfter(dueAfter);
        searchRequest.setTitleContains(titleContains);

        boolean sortByPriority = pageable.getSort().stream()
                .anyMatch(o -> "priority".equals(o.getProperty()));

        Page<Ticket> ticketPage = sortByPriority
                ? searchWithPrioritySort(searchRequest, pageable)
                : ticketRepository.findAll(buildSpecification(searchRequest), pageable);

        return ticketPage.map(ticketMapper::toDto);
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
    public TicketDto getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return ticketMapper.toDto(ticket);
    }

    @Override
    @Transactional
    public TicketDto createTicket(TicketCreateRequest request) {
        log.info("Creating ticket: title={}, projectId={}", request.getTitle(), request.getProjectId());

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

        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TicketDto updateTicket(Long id, TicketPutRequest request) {
        log.info("Full update (PUT) ticket id: {}", id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new UserNotFoundException(request.getAssigneeId()));
        }
        ticket.setAssignee(assignee);

        ticketMapper.replaceFromRequest(request, ticket);

        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toDto(updated);
    }

    @Override
    @Transactional
    public TicketDto partialUpdateTicket(Long id, TicketPatchRequest request) {
        log.info("Partial update (PATCH) ticket id: {}", id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        JsonNullable<Long> assigneeIdNullable = request.getAssigneeId();

        if (assigneeIdNullable != null && assigneeIdNullable.isPresent()) {
            Long assigneeId = assigneeIdNullable.orElse(null);

            ticket.setAssignee(
                    assigneeId == null
                            ? null
                            : userRepository.findById(assigneeId)
                            .orElseThrow(() -> new UserNotFoundException(assigneeId))
            );
        }

        ticketMapper.patchFromRequest(request, ticket);

        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticketRepository.delete(ticket);
    }
}
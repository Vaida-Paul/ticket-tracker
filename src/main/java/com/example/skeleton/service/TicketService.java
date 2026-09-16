package com.example.skeleton.service;

import com.example.skeleton.domain.model.Ticket;
import org.openapitools.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TicketService {

    /**
     2
     * Searches tickets using the provided filter criteria and returns a paginated result.
     3
     *
     4
     * @param status the ticket status to filter by
    5
     * @param priority the ticket priority to filter by
    6
     * @param assigneeId the ID of the assigned user to filter by
    7
     * @param dueBefore returns only tickets with a due time before the specified value
    8
     * @param dueAfter returns only tickets with a due time after the specified value
    9
     * @param titleContains text that must be contained in the ticket title
    10
     * @param page the page number to retrieve
    11
     * @param size the number of tickets per page
    12
     * @param sortBy the field used for sorting the results
    13
     * @param sortDirection the sorting direction (e.g. ASC or DESC)
    14
     * @return a page of matching ticket DTOs
    15
     */
    Page<TicketDto> searchTickets(
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
    );

    /**
     * Retrieves a ticket by its ID.
     *
     * @param id the ID of the ticket
     * @return the ticket
     * @throws com.example.skeleton.exception.TicketNotFoundException if the ticket does not exist
     */
    TicketDto getTicketById(Long id);

    /**
     * Creates a new ticket.
     *
     * @param request the ticket data (title, priority, projectId, assigneeId, dueDate)
     * @return the created ticket
     * @throws com.example.skeleton.exception.ProjectNotFoundException if the project does not exist
     * @throws com.example.skeleton.exception.UserNotFoundException    if the assignee does not exist
     */
    TicketDto createTicket(TicketCreateRequest request);

    /**
     * Performs a full update (PUT) of a ticket.
     * <p>
     * All fields are replaced, including setting missing fields to {@code null}.
     *
     * @param id      the ID of the ticket to update
     * @param request the new ticket data
     * @return the updated ticket
     * @throws com.example.skeleton.exception.TicketNotFoundException if the ticket does not exist
     * @throws com.example.skeleton.exception.UserNotFoundException   if the assignee does not exist
     */
    TicketDto updateTicket(Long id, TicketPutRequest request);

    /**
     * Performs a partial update (PATCH) of a ticket.
     * <p>
     * Only non-null fields from the request are applied to the existing ticket.
     *
     * @param id      the ID of the ticket to update
     * @param request the fields to update
     * @return the updated ticket
     * @throws com.example.skeleton.exception.TicketNotFoundException if the ticket does not exist
     * @throws com.example.skeleton.exception.UserNotFoundException   if the assignee does not exist
     */
    TicketDto partialUpdateTicket(Long id, TicketPatchRequest request);

    /**
     * Deletes a ticket by its ID.
     *
     * @param id the ID of the ticket to delete
     * @throws com.example.skeleton.exception.TicketNotFoundException if the ticket does not exist
     */
    void deleteTicket(Long id);
}
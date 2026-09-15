package com.example.skeleton.service;

import com.example.skeleton.domain.model.Comment;
import org.openapitools.model.CommentCreateRequest;
import org.openapitools.model.CommentDto;

import java.util.List;

public interface CommentService {

    /**
     * Retrieves all comments associated with a specific ticket.
     *
     * @param ticketId the ID of the ticket
     * @return list of comment DTOs for the given ticket
     * @throws com.example.skeleton.exception.TicketNotFoundException if the ticket does not exist
     */
    List<CommentDto> getCommentsByTicketId(Long ticketId);

    /**
     * Adds a new comment to a ticket.
     *
     * @param ticketId the ID of the ticket to comment on
     * @param request  the comment data (body)
     * @param userId   the ID of the user authoring the comment
     * @return the created comment DTO
     * @throws com.example.skeleton.exception.TicketNotFoundException if the ticket does not exist
     * @throws com.example.skeleton.exception.UserNotFoundException   if the user does not exist
     */
    CommentDto addComment(Long ticketId, CommentCreateRequest request, Long userId);

    /**
     * Deletes a comment by its ID.
     *
     * @param commentId the ID of the comment to delete
     * @throws com.example.skeleton.exception.CommentNotFoundException if the comment does not exist
     */
    void deleteComment(Long commentId);
}
package com.example.skeleton.service;

import com.example.skeleton.domain.model.Comment;
import org.openapitools.model.CommentCreateRequest;

import java.util.List;

public interface CommentService {
    List<Comment> getCommentsByTicketId(Long ticketId);
    Comment addComment(Long ticketId, CommentCreateRequest request, Long userId);
    void deleteComment(Long commentId);
}
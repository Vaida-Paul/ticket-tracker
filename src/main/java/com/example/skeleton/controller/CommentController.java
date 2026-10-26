package com.example.skeleton.controller;

import com.example.skeleton.security.SecurityUtils;
import com.example.skeleton.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.CommentsApi;
import org.openapitools.model.CommentCreateRequest;
import org.openapitools.model.CommentDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class CommentController implements CommentsApi {

    private final CommentService commentService;

    @Override
    public ResponseEntity<List<CommentDto>> getTicketComments(Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsByTicketId(ticketId));
    }

    @Override
    public ResponseEntity<CommentDto> addComment(Long ticketId, @Valid CommentCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommentDto comment = commentService.addComment(ticketId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @Override
    public ResponseEntity<Void> deleteComment(Long ticketId, Long commentId) {
        commentService.deleteComment(ticketId, commentId);
        return ResponseEntity.noContent().build();
    }
}
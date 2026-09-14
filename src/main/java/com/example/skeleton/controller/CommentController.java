package com.example.skeleton.controller;

import com.example.skeleton.domain.model.Comment;
import com.example.skeleton.mapper.CommentMapper;
import com.example.skeleton.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.CommentsApi;
import org.openapitools.model.CommentCreateRequest;
import org.openapitools.model.CommentDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class CommentController implements CommentsApi {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    private static final Long DEFAULT_USER_ID = 1L;

    @Override
    public ResponseEntity<List<CommentDto>> getTicketComments(Long ticketId) {
        List<Comment> comments = commentService.getCommentsByTicketId(ticketId);
        List<CommentDto> commentDtos = comments.stream()
                .map(commentMapper::toDto)
                .toList();
        return ResponseEntity.ok(commentDtos);
    }

    @Override
    public ResponseEntity<CommentDto> addComment(Long ticketId,@Valid CommentCreateRequest request) {
        Comment comment = commentService.addComment(ticketId, request,DEFAULT_USER_ID );
        return ResponseEntity.status(HttpStatus.CREATED).body(commentMapper.toDto(comment));
    }

    @Override
    public ResponseEntity<Void> deleteComment(Long ticketId, Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}

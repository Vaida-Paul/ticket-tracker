package com.example.skeleton.service.implementation;

import com.example.skeleton.domain.model.Comment;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.CommentNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.mapper.CommentMapper;
import com.example.skeleton.repository.CommentRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.CommentCreateRequest;
import org.openapitools.model.CommentDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByTicketId(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        return commentRepository.findByTicket(ticket).stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long ticketId, CommentCreateRequest request, Long userId) {
        log.info("Adding comment to ticket id: {} by user id: {}", ticketId, userId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Comment comment = commentMapper.toEntity(request);
        comment.setTicket(ticket);
        comment.setAuthor(author);

        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteComment(Long ticketId, Long commentId) {
        log.info("Deleting comment id: {} from ticket id: {}", commentId, ticketId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getTicket().getId().equals(ticketId)) {
            throw new CommentNotFoundException(commentId);
        }

        commentRepository.delete(comment);
    }
}
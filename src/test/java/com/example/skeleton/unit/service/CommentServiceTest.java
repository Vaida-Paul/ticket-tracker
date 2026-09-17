package com.example.skeleton.unit.service;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Comment;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.CommentNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.mapper.CommentMapper;
import com.example.skeleton.repository.CommentRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.implementation.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.CommentCreateRequest;
import org.openapitools.model.CommentDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user;
    private Ticket ticket;
    private Comment comment;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .build();
        user.setId(1L);

        Project project = Project.builder()
                .name("Test Project")
                .createdBy(user)
                .build();
        project.setId(1L);

        ticket = Ticket.builder()
                .title("Test Ticket")
                .status(TicketStatus.OPEN)
                .priority(Priority.HIGH)
                .project(project)
                .assignee(user)
                .build();
        ticket.setId(1L);

        comment = Comment.builder()
                .ticket(ticket)
                .author(user)
                .body("Test comment")
                .build();
        comment.setId(1L);

        commentDto = new CommentDto();
        commentDto.setId(1L);
        commentDto.setBody("Test comment");
        commentDto.setTicketId(1L);
        commentDto.setAuthorId(1L);
    }

    @Test
    void whenTicketExists_thenReturnsCommentDtos() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket(ticket)).thenReturn(List.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        List<CommentDto> result = commentService.getCommentsByTicketId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBody()).isEqualTo("Test comment");
        assertThat(result.get(0).getTicketId()).isEqualTo(1L);

        verify(ticketRepository).findById(1L);
        verify(commentRepository).findByTicket(ticket);
        verify(commentMapper).toDto(comment);

        verifyNoMoreInteractions(ticketRepository, commentRepository, commentMapper);
        verifyNoInteractions(userRepository);
    }

    @Test
    void whenTicketExistsButNoComments_thenReturnsEmptyList() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket(ticket)).thenReturn(List.of());

        List<CommentDto> result = commentService.getCommentsByTicketId(1L);

        assertThat(result).isEmpty();

        verify(ticketRepository).findById(1L);
        verify(commentRepository).findByTicket(ticket);

        verifyNoMoreInteractions(ticketRepository, commentRepository);
        verifyNoInteractions(userRepository, commentMapper);
    }

    @Test
    void whenTicketNotFound_thenThrowsException() {
        when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentsByTicketId(99999L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99999");

        verify(ticketRepository).findById(99999L);
        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(commentRepository, userRepository, commentMapper);
    }

    @Test
    void whenAddComment_thenReturnsCommentDto() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("New comment");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentMapper.toEntity(request)).thenReturn(comment);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.addComment(1L, request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isEqualTo("Test comment");
        assertThat(comment.getTicket()).isEqualTo(ticket);
        assertThat(comment.getAuthor()).isEqualTo(user);

        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(commentMapper).toEntity(request);
        verify(commentRepository).save(comment);
        verify(commentMapper).toDto(comment);

        verifyNoMoreInteractions(ticketRepository, userRepository, commentMapper, commentRepository);
    }

    @Test
    void whenAddCommentToNonExistentTicket_thenThrowsException() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("New comment");

        when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(99999L, request, 1L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99999");

        verify(ticketRepository).findById(99999L);
        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(userRepository, commentMapper, commentRepository);
    }

    @Test
    void whenAddCommentWithNonExistentUser_thenThrowsException() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("New comment");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(1L, request, 99999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99999");

        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(99999L);

        verifyNoMoreInteractions(ticketRepository, userRepository);
        verifyNoInteractions(commentMapper, commentRepository);
    }

    @Test
    void whenDeleteComment_thenDeletesSuccessfully() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).delete(comment);

        commentService.deleteComment(1L);

        verify(commentRepository).findById(1L);
        verify(commentRepository).delete(comment);

        verifyNoMoreInteractions(commentRepository);
        verifyNoInteractions(ticketRepository, userRepository, commentMapper);
    }

    @Test
    void whenDeleteNonExistentComment_thenThrowsException() {
        when(commentRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(99999L))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessageContaining("99999");

        verify(commentRepository).findById(99999L);
        verifyNoMoreInteractions(commentRepository);
        verifyNoInteractions(ticketRepository, userRepository, commentMapper);
    }
}
package com.example.skeleton.unit.service;

import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.TicketNotFoundException;
import com.example.skeleton.mapper.EnumMapper;
import com.example.skeleton.mapper.TicketMapper;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.implementation.TicketServiceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.TicketCreateRequest;
import org.openapitools.model.TicketSearchRequest;
import org.openapitools.model.TicketUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private EnumMapper enumMapper;

    @InjectMocks
    private TicketServiceImp ticketService;

    private User user;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .build();
        user.setId(1L);

        project = Project.builder()
                .name("Test Project")
                .description("Description")
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
    }

    @Test
    void whenTicketExists_thenReturnsTicket() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Ticket result = ticketService.getTicketById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Ticket");

        verify(ticketRepository).findById(1L);
        verifyNoMoreInteractions(ticketRepository);
    }

    @Test
    void whenTicketDoesNotExist_thenThrowsException() {
        when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99999L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99999");

        verify(ticketRepository).findById(99999L);
        verifyNoMoreInteractions(ticketRepository);
    }

    @Test
    void whenCreateTicketWithValidData_thenReturnsSavedTicket() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("New Ticket");
        request.setPriority(org.openapitools.model.Priority.HIGH);
        request.setProjectId(1L);
        request.setAssigneeId(1L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketMapper.toEntity(request)).thenReturn(ticket);
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket result = ticketService.createTicket(request);

        assertThat(result).isNotNull();
        assertThat(result.getProject()).isEqualTo(project);
        assertThat(result.getAssignee()).isEqualTo(user);

        verify(projectRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(ticketMapper).toEntity(request);
        verify(ticketRepository).save(ticket);

        verifyNoMoreInteractions(projectRepository, userRepository, ticketMapper, ticketRepository);
        verifyNoInteractions(enumMapper);
    }

    @Test
    void whenCreateTicketWithNonExistentProject_thenThrowsException() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("New Ticket");
        request.setPriority(org.openapitools.model.Priority.HIGH);
        request.setProjectId(99999L);

        when(projectRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("99999");

        verify(projectRepository).findById(99999L);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, ticketMapper, ticketRepository, enumMapper);
    }

    @Test
    void whenCreateTicketWithoutAssignee_thenSavesTicketWithoutAssignee() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("New Ticket");
        request.setPriority(org.openapitools.model.Priority.HIGH);
        request.setProjectId(1L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketMapper.toEntity(request)).thenReturn(ticket);
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket result = ticketService.createTicket(request);

        assertThat(result).isNotNull();
        assertThat(result.getAssignee()).isNull();

        verify(projectRepository).findById(1L);
        verify(ticketMapper).toEntity(request);
        verify(ticketRepository).save(ticket);

        verifyNoMoreInteractions(projectRepository, ticketMapper, ticketRepository);
        verifyNoInteractions(userRepository, enumMapper);
    }

    @Test
    void whenUpdateTicketWithValidData_thenReturnsUpdatedTicket() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setTitle("Updated Title");
        request.setStatus(org.openapitools.model.TicketStatus.IN_PROGRESS);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        doNothing().when(ticketMapper).updateFromRequest(request, ticket);

        Ticket result = ticketService.updateTicket(1L, request);

        assertThat(result).isNotNull();

        verify(ticketRepository).findById(1L);
        verify(ticketMapper).updateFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);

        verifyNoMoreInteractions(ticketRepository, ticketMapper);
        verifyNoInteractions(projectRepository, userRepository, enumMapper);
    }

    @Test
    void whenUpdateNonExistentTicket_thenThrowsException() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setTitle("Updated Title");

        when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.updateTicket(99999L, request))
                .isInstanceOf(TicketNotFoundException.class);

        verify(ticketRepository).findById(99999L);
        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(ticketMapper, projectRepository, userRepository, enumMapper);
    }

    @Test
    void whenUpdateTicketWithNewAssignee_thenAssigneeUpdated() {
        User newAssignee = User.builder()
                .email("new@example.com")
                .fullName("New User")
                .build();
        newAssignee.setId(2L);

        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setAssigneeId(2L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newAssignee));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        doNothing().when(ticketMapper).updateFromRequest(request, ticket);

        ticketService.updateTicket(1L, request);

        assertThat(ticket.getAssignee()).isEqualTo(newAssignee);

        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(ticketMapper).updateFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);

        verifyNoMoreInteractions(ticketRepository, userRepository, ticketMapper);
        verifyNoInteractions(projectRepository, enumMapper);
    }

    @Test
    void whenDeleteTicket_thenDeletesSuccessfully() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        doNothing().when(ticketRepository).delete(ticket);

        ticketService.deleteTicket(1L);

        verify(ticketRepository).findById(1L);
        verify(ticketRepository).delete(ticket);

        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(ticketMapper, projectRepository, userRepository, enumMapper);
    }

    @Test
    void whenDeleteNonExistentTicket_thenThrowsException() {
        when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.deleteTicket(99999L))
                .isInstanceOf(TicketNotFoundException.class);

        verify(ticketRepository).findById(99999L);
        verifyNoMoreInteractions(ticketRepository);
    }

    @Test
    void whenSearchTickets_thenReturnsPage() {
        TicketSearchRequest searchRequest = new TicketSearchRequest();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> expectedPage = new PageImpl<>(List.of(ticket));

        when(enumMapper.toDomainTicketStatus(null)).thenReturn(null);
        when(enumMapper.toDomainPriority(null)).thenReturn(null);
        when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<Ticket> result = ticketService.searchTickets(searchRequest, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        ArgumentCaptor<Specification<Ticket>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        verify(enumMapper).toDomainTicketStatus(null);
        verify(enumMapper).toDomainPriority(null);
        verify(ticketRepository).findAll(specCaptor.capture(), eq(pageable));

        verifyNoMoreInteractions(ticketRepository, enumMapper);
        verifyNoInteractions(userRepository, projectRepository, ticketMapper);
    }
}
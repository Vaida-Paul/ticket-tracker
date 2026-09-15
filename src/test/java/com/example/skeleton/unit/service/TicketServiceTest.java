package com.example.skeleton.unit.service;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Role;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private TicketDto ticketDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.MEMBER)
                .authProvider(AuthProvider.LOCAL)
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
                .status(com.example.skeleton.domain.enums.TicketStatus.OPEN)
                .priority(com.example.skeleton.domain.enums.Priority.HIGH)
                .project(project)
                .assignee(user)
                .build();
        ticket.setId(1L);

        ticketDto = new TicketDto();
        ticketDto.setId(1L);
        ticketDto.setTitle("Test Ticket");
        ticketDto.setStatus(TicketStatus.OPEN);
        ticketDto.setPriority(Priority.HIGH);
        ticketDto.setProjectId(1L);
    }

    @Test
    void whenTicketExists_thenReturnsTicketDto() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);

        TicketDto result = ticketService.getTicketById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Ticket");

        verify(ticketRepository).findById(1L);
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(ticketRepository, ticketMapper);
        verifyNoInteractions(userRepository, projectRepository, enumMapper);
    }

    @Test
    void whenTicketDoesNotExist_thenThrowsException() {
        when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99999L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("99999");

        verify(ticketRepository).findById(99999L);
        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(ticketMapper, userRepository, projectRepository, enumMapper);
    }

    @Test
    void whenCreateTicketWithValidData_thenReturnsTicketDto() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("New Ticket");
        request.setPriority(Priority.HIGH);
        request.setProjectId(1L);
        request.setAssigneeId(1L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketMapper.toEntity(request)).thenReturn(ticket);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);

        TicketDto result = ticketService.createTicket(request);

        assertThat(result).isNotNull();
        assertThat(ticket.getProject()).isEqualTo(project);
        assertThat(ticket.getAssignee()).isEqualTo(user);

        verify(projectRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(ticketMapper).toEntity(request);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(projectRepository, userRepository, ticketMapper, ticketRepository);
        verifyNoInteractions(enumMapper);
    }

    @Test
    void whenCreateTicketWithNonExistentProject_thenThrowsException() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("New Ticket");
        request.setPriority(Priority.HIGH);
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
        request.setPriority(Priority.HIGH);
        request.setProjectId(1L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketMapper.toEntity(request)).thenReturn(ticket);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);

        TicketDto result = ticketService.createTicket(request);

        assertThat(result).isNotNull();
        assertThat(ticket.getAssignee()).isNull();

        verify(projectRepository).findById(1L);
        verify(ticketMapper).toEntity(request);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(projectRepository, ticketMapper, ticketRepository);
        verifyNoInteractions(userRepository, enumMapper);
    }

    @Test
    void whenUpdateTicketWithValidData_thenReturnsTicketDto() {
        TicketPutRequest request = new TicketPutRequest();
        request.setTitle("Updated Title");
        request.setStatus(TicketStatus.IN_PROGRESS);
        request.setPriority(Priority.HIGH);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);
        doNothing().when(ticketMapper).replaceFromRequest(request, ticket);

        TicketDto result = ticketService.updateTicket(1L, request);

        assertThat(result).isNotNull();

        verify(ticketRepository).findById(1L);
        verify(ticketMapper).replaceFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(ticketRepository, ticketMapper);
        verifyNoInteractions(projectRepository, userRepository, enumMapper);
    }

    @Test
    void whenUpdateNonExistentTicket_thenThrowsException() {
        TicketPutRequest request = new TicketPutRequest();
        request.setTitle("Updated Title");
        request.setStatus(TicketStatus.IN_PROGRESS);
        request.setPriority(Priority.HIGH);

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

        TicketPutRequest request = new TicketPutRequest();
        request.setTitle("Updated Title");
        request.setStatus(TicketStatus.IN_PROGRESS);
        request.setPriority(Priority.HIGH);
        request.setAssigneeId(2L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newAssignee));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);
        doNothing().when(ticketMapper).replaceFromRequest(request, ticket);

        ticketService.updateTicket(1L, request);

        assertThat(ticket.getAssignee()).isEqualTo(newAssignee);

        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(ticketMapper).replaceFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(ticketRepository, userRepository, ticketMapper);
        verifyNoInteractions(projectRepository, enumMapper);
    }

    @Test
    void whenPatchTicketWithValidData_thenReturnsTicketDto() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setStatus(TicketStatus.DONE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);
        doNothing().when(ticketMapper).patchFromRequest(request, ticket);

        TicketDto result = ticketService.partialUpdateTicket(1L, request);

        assertThat(result).isNotNull();

        verify(ticketRepository).findById(1L);
        verify(ticketMapper).patchFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(ticketRepository, ticketMapper);
        verifyNoInteractions(projectRepository, userRepository, enumMapper);
    }

    @Test
    void whenPatchNonExistentTicket_thenThrowsException() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setStatus(TicketStatus.DONE);

        when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.partialUpdateTicket(99999L, request))
                .isInstanceOf(TicketNotFoundException.class);

        verify(ticketRepository).findById(99999L);
        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(ticketMapper, projectRepository, userRepository, enumMapper);
    }

    @Test
    void whenPatchTicketWithNewAssignee_thenAssigneeUpdated() {
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
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);
        doNothing().when(ticketMapper).patchFromRequest(request, ticket);

        ticketService.partialUpdateTicket(1L, request);

        assertThat(ticket.getAssignee()).isEqualTo(newAssignee);

        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(ticketMapper).patchFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

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
    void whenSearchTicketsWithNoFilters_thenReturnsPage() {
        Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket));
        Page<TicketDto> expectedPage = new PageImpl<>(List.of(ticketDto));

        when(enumMapper.toDomainTicketStatus(null)).thenReturn(null);
        when(enumMapper.toDomainPriority(null)).thenReturn(null);
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(ticketPage);
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);

        Page<TicketDto> result = ticketService.searchTickets(
                null, null, null, null, null, null,
                0, 10, "createdAt", "desc"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Ticket");

        verify(enumMapper).toDomainTicketStatus(null);
        verify(enumMapper).toDomainPriority(null);
        verify(ticketRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(ticketRepository, enumMapper, ticketMapper);
        verifyNoInteractions(userRepository, projectRepository);
    }

    @Test
    void whenSearchTicketsWithFilters_thenReturnsPage() {
        Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket));

        when(enumMapper.toDomainTicketStatus(TicketStatus.OPEN))
                .thenReturn(com.example.skeleton.domain.enums.TicketStatus.OPEN);
        when(enumMapper.toDomainPriority(Priority.HIGH))
                .thenReturn(com.example.skeleton.domain.enums.Priority.HIGH);
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(ticketPage);
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);

        Page<TicketDto> result = ticketService.searchTickets(
                TicketStatus.OPEN, Priority.HIGH, 1L,
                LocalDate.now(), LocalDate.now().plusDays(7), "test",
                0, 10, "createdAt", "desc"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(enumMapper).toDomainTicketStatus(TicketStatus.OPEN);
        verify(enumMapper).toDomainPriority(Priority.HIGH);
        verify(ticketRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(ticketRepository, enumMapper, ticketMapper);
        verifyNoInteractions(userRepository, projectRepository);
    }

    @Test
    void whenSearchTicketsSortByPriority_thenReturnsSortedInMemory() {
        Ticket ticketLow = Ticket.builder()
                .title("Low")
                .priority(com.example.skeleton.domain.enums.Priority.LOW)
                .project(project)
                .build();
        ticketLow.setId(2L);

        Ticket ticketUrgent = Ticket.builder()
                .title("Urgent")
                .priority(com.example.skeleton.domain.enums.Priority.URGENT)
                .project(project)
                .build();
        ticketUrgent.setId(3L);

        when(ticketMapper.toDto(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            TicketDto dto = new TicketDto();
            dto.setId(t.getId());
            dto.setTitle(t.getTitle());
            dto.setPriority(switch (t.getPriority()) {
                case LOW -> Priority.LOW;
                case MEDIUM -> Priority.MEDIUM;
                case HIGH -> Priority.HIGH;
                case URGENT -> Priority.URGENT;
            });
            return dto;
        });

        when(enumMapper.toDomainTicketStatus(null)).thenReturn(null);
        when(enumMapper.toDomainPriority(null)).thenReturn(null);
        when(ticketRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(ticketLow, ticket));

        Page<TicketDto> result = ticketService.searchTickets(
                null, null, null, null, null, null,
                0, 10, "priority", "desc"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent().get(0).getPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.getContent().get(1).getPriority()).isEqualTo(Priority.LOW);

        verify(enumMapper).toDomainTicketStatus(null);
        verify(enumMapper).toDomainPriority(null);
        verify(ticketRepository).findAll(any(Specification.class));
        verify(ticketMapper, times(2)).toDto(any(Ticket.class));

        verifyNoMoreInteractions(ticketRepository, enumMapper, ticketMapper);
        verifyNoInteractions(userRepository, projectRepository);
    }
}
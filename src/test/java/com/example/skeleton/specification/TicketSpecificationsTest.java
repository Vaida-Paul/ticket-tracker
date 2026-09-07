package com.example.skeleton.specification;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.domain.dto.TicketSearchRequest;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketSpecificationsTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TicketService ticketService;

    private User user1, user2;
    private Project project;


    private final LocalDate TODAY = LocalDate.of(2026, 9, 4);

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user1 = User.builder()
                .email("user1@example.com")
                .fullName("User One")
                .role(Role.MEMBER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
                .email("user2@example.com")
                .fullName("User Two")
                .role(Role.MEMBER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        user2 = userRepository.save(user2);

        project = Project.builder()
                .name("Test Project")
                .description("Project for testing")
                .createdBy(user1)
                .build();
        project = projectRepository.save(project);

        Ticket ticket1 = Ticket.builder()
                .title("Open High Ticket")
                .description("Description 1")
                .status(TicketStatus.OPEN)
                .priority(Priority.HIGH)
                .project(project)
                .assignee(user1)
                .dueDate(TODAY.plusDays(5).atStartOfDay())
                .build();
        ticketRepository.save(ticket1);

        Ticket ticket2 = Ticket.builder()
                .title("In Progress Medium Ticket")
                .description("Description 2")
                .status(TicketStatus.IN_PROGRESS)
                .priority(Priority.MEDIUM)
                .project(project)
                .assignee(user1)
                .dueDate(TODAY.plusDays(10).atStartOfDay())
                .build();
        ticketRepository.save(ticket2);

        Ticket ticket3 = Ticket.builder()
                .title("Open Low Ticket")
                .description("Description 3")
                .status(TicketStatus.OPEN)
                .priority(Priority.LOW)
                .project(project)
                .assignee(user2)
                .dueDate(TODAY.plusDays(15).atStartOfDay())
                .build();
        ticketRepository.save(ticket3);

        Ticket ticket4 = Ticket.builder()
                .title("Done Urgent Ticket")
                .description("Description 4")
                .status(TicketStatus.DONE)
                .priority(Priority.URGENT)
                .project(project)
                .assignee(user2)
                .dueDate(TODAY.minusDays(1).atStartOfDay())
                .build();
        ticketRepository.save(ticket4);

        Ticket ticket5 = Ticket.builder()
                .title("Open Urgent Ticket")
                .description("Description 5")
                .status(TicketStatus.OPEN)
                .priority(Priority.URGENT)
                .project(project)
                .assignee(user1)
                .dueDate(TODAY.plusDays(2).atStartOfDay())
                .build();
        ticketRepository.save(ticket5);
    }

    @Test
    void filterByStatusOnly() {
        TicketSearchRequest request = TicketSearchRequest.builder()
                .status(TicketStatus.OPEN)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> result = ticketService.searchTickets(request, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(t -> t.getStatus() == TicketStatus.OPEN);
    }

    @Test
    void filterByStatusAndPriority() {
        TicketSearchRequest request = TicketSearchRequest.builder()
                .status(TicketStatus.OPEN)
                .priority(Priority.HIGH)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> result = ticketService.searchTickets(request, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Open High Ticket");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(result.getContent().get(0).getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void filterWithNoParamsReturnsEverything() {
        TicketSearchRequest request = TicketSearchRequest.builder().build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> result = ticketService.searchTickets(request, pageable);

        assertThat(result.getContent()).hasSize(5);
    }

    @Test
    void filterByDueBeforeAndDueAfter() {
        TicketSearchRequest request = TicketSearchRequest.builder()
                .dueAfter(TODAY.minusDays(3))
                .dueBefore(TODAY.plusDays(7))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> result = ticketService.searchTickets(request, pageable);

        assertThat(result.getContent()).hasSize(3);

        assertThat(result.getContent())
                .extracting(Ticket::getTitle)
                .containsExactlyInAnyOrder("Open High Ticket", "Done Urgent Ticket", "Open Urgent Ticket");
    }

    @Test
    void filterWithMultipleCriteria() {
        TicketSearchRequest request = TicketSearchRequest.builder()
                .status(TicketStatus.OPEN)
                .assigneeId(user1.getId())
                .priority(Priority.URGENT)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> result = ticketService.searchTickets(request, pageable);

        assertThat(result.getContent()).hasSize(1);
        Ticket ticket = result.getContent().get(0);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getAssignee().getId()).isEqualTo(user1.getId());
        assertThat(ticket.getPriority()).isEqualTo(Priority.URGENT);
        assertThat(ticket.getTitle()).isEqualTo("Open Urgent Ticket");
    }

    @Test
    void filterWithPaginationAndSorting() {
        TicketSearchRequest request = TicketSearchRequest.builder()
                .status(TicketStatus.OPEN)
                .build();

        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> result = ticketService.searchTickets(request, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);

        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Open Urgent Ticket");
    }

    @Test
    void testTitleContains() {
        TicketSearchRequest request = TicketSearchRequest.builder()
                .titleContains("urgent")
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> result = ticketService.searchTickets(request, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Ticket::getTitle)
                .allMatch(title -> title.toLowerCase().contains("urgent"));
    }
}
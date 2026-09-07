package com.example.skeleton.specification;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketSpecificationBuilderTest {

    @Autowired
    private TicketSpecificationBuilder builder;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

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
    void testBuilderWithAllFilters() {
        Specification<Ticket> spec = builder
                .reset()
                .withStatus(TicketStatus.OPEN)
                .withPriority(Priority.HIGH)
                .withDueBefore(TODAY.plusDays(7))
                .build();

        assertThat(spec).isNotNull();

        List<Ticket> results = ticketRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Open High Ticket");
    }

    @Test
    void testBuilderWithNullFilters() {
        Specification<Ticket> spec = builder
                .reset()
                .withStatus(null)
                .withPriority(null)
                .withAssignee(null)
                .withDueBefore(null)
                .withDueAfter(null)
                .withTitleContains(null)
                .build();

        assertThat(spec).isNotNull();

        List<Ticket> results = ticketRepository.findAll(spec);
        assertThat(results).hasSize(5);
    }

    @Test
    void testPaginationAndSorting() {
        Pageable pageable = PageRequest.of(
                0,
                2,
                Sort.by(Sort.Direction.ASC, "dueDate")
        );

        Page<Ticket> result = ticketRepository.findAll(
                Specification.allOf(),
                pageable
        );

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);

        assertThat(result.getContent().get(0).getDueDate())
                .isBeforeOrEqualTo(result.getContent().get(1).getDueDate());
    }

    @Test
    void testBuilderWithPartialFilters() {
        Specification<Ticket> spec = builder
                .reset()
                .withStatus(TicketStatus.OPEN)
                .withPriority(null)
                .withAssignee(user1.getId())
                .withDueBefore(null)
                .build();

        assertThat(spec).isNotNull();

        List<Ticket> results = ticketRepository.findAll(spec);
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Ticket::getTitle)
                .containsExactlyInAnyOrder("Open High Ticket", "Open Urgent Ticket");
    }

    @Test
    void testBuilderWithDateRange() {
        Specification<Ticket> spec = builder
                .reset()
                .withDueAfter(TODAY.minusDays(3))
                .withDueBefore(TODAY.plusDays(7))
                .build();

        List<Ticket> results = ticketRepository.findAll(spec);
        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(Ticket::getTitle)
                .containsExactlyInAnyOrder("Open High Ticket", "Open Urgent Ticket", "Done Urgent Ticket");
    }

    @Test
    void testBuilderWithTitleContains() {
        Specification<Ticket> spec = builder
                .reset()
                .withTitleContains("urgent")
                .build();

        List<Ticket> results = ticketRepository.findAll(spec);
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Ticket::getTitle)
                .containsExactlyInAnyOrder("Done Urgent Ticket", "Open Urgent Ticket");
    }

    @Test
    void testBuilderWithMultipleFilters() {
        Specification<Ticket> spec = builder
                .reset()
                .withStatus(TicketStatus.OPEN)
                .withPriority(Priority.URGENT)
                .withAssignee(user1.getId())
                .build();

        List<Ticket> results = ticketRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Open Urgent Ticket");
    }

    @Test
    void testBuilderResetWorks() {

        Specification<Ticket> spec1 = builder
                .reset()
                .withStatus(TicketStatus.OPEN)
                .build();

        Specification<Ticket> spec2 = builder
                .reset()
                .withStatus(TicketStatus.DONE)
                .build();

        List<Ticket> openTickets = ticketRepository.findAll(spec1);
        List<Ticket> doneTickets = ticketRepository.findAll(spec2);

        assertThat(openTickets).hasSize(3);
        assertThat(doneTickets).hasSize(1);
        assertThat(doneTickets.get(0).getTitle()).isEqualTo("Done Urgent Ticket");
    }
}
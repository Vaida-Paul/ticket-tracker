package com.example.skeleton.specification;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.Priority;
import org.openapitools.model.TicketDto;
import org.openapitools.model.TicketStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class TicketSpecificationsTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("ticket_tracker_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

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
                .status(com.example.skeleton.domain.enums.TicketStatus.OPEN)
                .priority(com.example.skeleton.domain.enums.Priority.HIGH)
                .project(project)
                .assignee(user1)
                .dueDate(TODAY.plusDays(5).atStartOfDay().atOffset(ZoneOffset.UTC))
                .build();
        ticketRepository.save(ticket1);

        Ticket ticket2 = Ticket.builder()
                .title("In Progress Medium Ticket")
                .description("Description 2")
                .status(com.example.skeleton.domain.enums.TicketStatus.IN_PROGRESS)
                .priority(com.example.skeleton.domain.enums.Priority.MEDIUM)
                .project(project)
                .assignee(user1)
                .dueDate(TODAY.plusDays(10).atStartOfDay().atOffset(ZoneOffset.UTC))
                .build();
        ticketRepository.save(ticket2);

        Ticket ticket3 = Ticket.builder()
                .title("Open Low Ticket")
                .description("Description 3")
                .status(com.example.skeleton.domain.enums.TicketStatus.OPEN)
                .priority(com.example.skeleton.domain.enums.Priority.LOW)
                .project(project)
                .assignee(user2)
                .dueDate(TODAY.plusDays(15).atStartOfDay().atOffset(ZoneOffset.UTC))
                .build();
        ticketRepository.save(ticket3);

        Ticket ticket4 = Ticket.builder()
                .title("Done Urgent Ticket")
                .description("Description 4")
                .status(com.example.skeleton.domain.enums.TicketStatus.DONE)
                .priority(com.example.skeleton.domain.enums.Priority.URGENT)
                .project(project)
                .assignee(user2)
                .dueDate(TODAY.minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))
                .build();
        ticketRepository.save(ticket4);

        Ticket ticket5 = Ticket.builder()
                .title("Open Urgent Ticket")
                .description("Description 5")
                .status(com.example.skeleton.domain.enums.TicketStatus.OPEN)
                .priority(com.example.skeleton.domain.enums.Priority.URGENT)
                .project(project)
                .assignee(user1)
                .dueDate(TODAY.plusDays(2).atStartOfDay().atOffset(ZoneOffset.UTC))
                .build();
        ticketRepository.save(ticket5);
    }

    @Test
    void filterByStatusOnly() {
        Page<TicketDto> result = ticketService.searchTickets(
                TicketStatus.OPEN, null, null, null, null, null,
                0, 10, "createdAt", "desc"
        );

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(t -> t.getStatus() == TicketStatus.OPEN);
    }

    @Test
    void filterByStatusAndPriority() {
        Page<TicketDto> result = ticketService.searchTickets(
                TicketStatus.OPEN, Priority.HIGH, null, null, null, null,
                0, 10, "createdAt", "desc"
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Open High Ticket");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(result.getContent().get(0).getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void filterWithNoParamsReturnsEverything() {
        Page<TicketDto> result = ticketService.searchTickets(
                null, null, null, null, null, null,
                0, 10, "createdAt", "desc"
        );

        assertThat(result.getContent()).hasSize(5);
    }

    @Test
    void filterByDueBeforeAndDueAfter() {
        Page<TicketDto> result = ticketService.searchTickets(
                null, null, null, TODAY.plusDays(7),
                TODAY.minusDays(3), null,
                0, 10, "createdAt", "desc"
        );

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(TicketDto::getTitle)
                .containsExactlyInAnyOrder("Open High Ticket", "Done Urgent Ticket", "Open Urgent Ticket");
    }

    @Test
    void filterWithMultipleCriteria() {
        Page<TicketDto> result = ticketService.searchTickets(
                TicketStatus.OPEN, Priority.URGENT, user1.getId(), null, null, null,
                0, 10, "createdAt", "desc"
        );

        assertThat(result.getContent()).hasSize(1);
        TicketDto ticket = result.getContent().get(0);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getAssigneeId()).isEqualTo(user1.getId());
        assertThat(ticket.getPriority()).isEqualTo(Priority.URGENT);
        assertThat(ticket.getTitle()).isEqualTo("Open Urgent Ticket");
    }

    @Test
    void filterWithPaginationAndSorting() {
        Page<TicketDto> result = ticketService.searchTickets(
                TicketStatus.OPEN, null, null, null, null, null,
                0, 2, "createdAt", "desc"
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testTitleContains() {
        Page<TicketDto> result = ticketService.searchTickets(
                null, null, null, null, null, "urgent",
                0, 10, "createdAt", "desc"
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(TicketDto::getTitle)
                .allMatch(title -> title.toLowerCase().contains("urgent"));
    }
}
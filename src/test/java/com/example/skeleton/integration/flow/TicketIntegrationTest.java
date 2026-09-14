package com.example.skeleton.integration.flow;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class TicketIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .email("integration.test@example.com")
                .fullName("Integration Tester")
                .role(Role.MEMBER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        user = userRepository.save(user);

        project = Project.builder()
                .name("Integration Test Project")
                .description("Project for integration testing")
                .createdBy(user)
                .build();
        project = projectRepository.save(project);
    }

    @Test
    void fullFlow_createGetUpdateDeleteTicket() throws Exception {
        String createBody = """
                {
                    "title": "Integration Test Ticket",
                    "description": "Created via integration test",
                    "priority": "HIGH",
                    "projectId": %d,
                    "assigneeId": %d
                }
                """.formatted(project.getId(), user.getId());

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Ticket"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        List<Ticket> tickets = ticketRepository.findAll();
        assertThat(tickets).hasSize(1);
        Long ticketId = tickets.get(0).getId();

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.title").value("Integration Test Ticket"));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ticketId))
                .andExpect(jsonPath("$.totalElements").value(1));

        String updateBody = """
                {
                    "title": "Updated Title",
                    "description": "Updated description",
                    "status": "IN_PROGRESS",
                    "priority": "URGENT",
                    "assigneeId": %d
                }
                """.formatted(user.getId());

        mockMvc.perform(put("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("URGENT"));

        Ticket updated = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(updated.getPriority()).isEqualTo(Priority.URGENT);

        String patchBody = """
                {
                    "status": "DONE"
                }
                """;

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.title").value("Updated Title"));

        Ticket patched = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(patched.getStatus()).isEqualTo(TicketStatus.DONE);
        assertThat(patched.getTitle()).isEqualTo("Updated Title");

        mockMvc.perform(delete("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isNoContent());

        assertThat(ticketRepository.findById(ticketId)).isEmpty();
    }

    @Test
    void filterFlow_filterTicketsByStatusAndPriority() throws Exception {
        createTicketDirectly("Open High", TicketStatus.OPEN, Priority.HIGH);
        createTicketDirectly("Open Urgent", TicketStatus.OPEN, Priority.URGENT);
        createTicketDirectly("In Progress High", TicketStatus.IN_PROGRESS, Priority.HIGH);
        createTicketDirectly("Done High", TicketStatus.DONE, Priority.HIGH);

        mockMvc.perform(get("/api/v1/tickets")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("status", "OPEN")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Open High"));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void filterFlow_filterByTitleContains() throws Exception {
        createTicketDirectly("Fix login bug", TicketStatus.OPEN, Priority.HIGH);
        createTicketDirectly("Fix logout bug", TicketStatus.OPEN, Priority.MEDIUM);
        createTicketDirectly("Add feature X", TicketStatus.OPEN, Priority.LOW);

        mockMvc.perform(get("/api/v1/tickets")
                        .param("titleContains", "fix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("titleContains", "logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix logout bug"));
    }

    @Test
    void filterFlow_filterByDateRange() throws Exception {
        createTicketWithDueDate("Due Soon", OffsetDateTime.now().plusDays(2));
        createTicketWithDueDate("Due Next Week", OffsetDateTime.now().plusDays(7));
        createTicketWithDueDate("Due Next Month", OffsetDateTime.now().plusDays(30));

        String dueBefore = java.time.LocalDate.now().plusDays(10).toString();

        mockMvc.perform(get("/api/v1/tickets")
                        .param("dueBefore", dueBefore))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void errorFlow_getNonExistentTicket_thenReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));
    }

    @Test
    void errorFlow_createTicketWithMissingProject_thenReturns404() throws Exception {
        String body = """
                {
                    "title": "Test",
                    "priority": "HIGH",
                    "projectId": 99999
                }
                """;

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project with id 99999 not found"));

        assertThat(ticketRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_createTicketWithInvalidData_thenReturns400() throws Exception {
        String body = """
                {
                    "title": "",
                    "priority": "HIGH",
                    "projectId": %d
                }
                """.formatted(project.getId());

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("title")));

        assertThat(ticketRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_createTicketWithInvalidEnum_thenReturns400() throws Exception {
        String body = """
                {
                    "title": "Test",
                    "priority": "INVALID_PRIORITY",
                    "projectId": %d
                }
                """.formatted(project.getId());

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed Request"));
    }

    @Test
    void errorFlow_createTicketWithMalformedJson_thenReturns400() throws Exception {
        String malformedJson = "{ \"title\": \"Test\", \"priority\": }";

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private Ticket createTicketDirectly(String title, TicketStatus status, Priority priority) {
        Ticket ticket = Ticket.builder()
                .title(title)
                .status(status)
                .priority(priority)
                .project(project)
                .assignee(user)
                .dueDate(OffsetDateTime.now().plusDays(7).withOffsetSameInstant(ZoneOffset.UTC))
                .build();
        return ticketRepository.save(ticket);
    }

    private Ticket createTicketWithDueDate(String title, OffsetDateTime dueDate) {
        Ticket ticket = Ticket.builder()
                .title(title)
                .status(TicketStatus.OPEN)
                .priority(Priority.MEDIUM)
                .project(project)
                .assignee(user)
                .dueDate(dueDate)
                .build();
        return ticketRepository.save(ticket);
    }
}
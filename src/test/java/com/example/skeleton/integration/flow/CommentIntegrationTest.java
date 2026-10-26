package com.example.skeleton.integration.flow;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Comment;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.repository.CommentRepository;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers
@Transactional
class CommentIntegrationTest {

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
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        entityManager.createNativeQuery(
                "INSERT INTO users (id, email, full_name, role, auth_provider, created_at, updated_at) " +
                        "VALUES (1, 'comment.tester@example.com', 'Comment Tester', 'PROJECT_MANAGER', 'LOCAL', NOW(), NOW())"
        ).executeUpdate();

        entityManager.flush();
        entityManager.clear();

        user = userRepository.findById(1L).orElseThrow();

        project = Project.builder()
                .name("Comment Test Project")
                .description("Project for comment testing")
                .createdBy(user)
                .build();
        project = projectRepository.save(project);

        ticket = Ticket.builder()
                .title("Test Ticket for Comments")
                .status(TicketStatus.OPEN)
                .priority(Priority.HIGH)
                .project(project)
                .assignee(user)
                .dueDate(OffsetDateTime.now().plusDays(7))
                .build();
        ticket = ticketRepository.save(ticket);

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                user.getEmail()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fullFlow_addGetDeleteComment() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        String addBody = """
                {
                    "body": "This is my first comment"
                }
                """;

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("This is my first comment"))
                .andExpect(jsonPath("$.ticketId").value(ticket.getId()))
                .andExpect(jsonPath("$.authorId").value(1));

        List<Comment> comments = commentRepository.findAll();
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getBody()).isEqualTo("This is my first comment");
        Long commentId = comments.get(0).getId();

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].body").value("This is my first comment"))
                .andExpect(jsonPath("$[0].ticketId").value(ticket.getId()));

        String addBody2 = """
                {
                    "body": "This is my second comment"
                }
                """;

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("This is my second comment"));

        assertThat(commentRepository.findAll()).hasSize(2);

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/comments/{commentId}",
                        ticket.getId(), commentId))
                .andExpect(status().isNoContent());

        assertThat(commentRepository.findAll()).hasSize(1);
        assertThat(commentRepository.findById(commentId)).isEmpty();

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("This is my second comment"));
    }

    @Test
    void multipleTicketsFlow_commentsAreIsolatedPerTicket() throws Exception {
        Ticket secondTicket = Ticket.builder()
                .title("Second Ticket")
                .status(TicketStatus.OPEN)
                .priority(Priority.LOW)
                .project(project)
                .assignee(user)
                .dueDate(OffsetDateTime.now().plusDays(5))
                .build();
        secondTicket = ticketRepository.save(secondTicket);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Comment on ticket 1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", secondTicket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Comment on ticket 2\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("Comment on ticket 1"));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", secondTicket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("Comment on ticket 2"));
    }

    @Test
    void multipleTicketsFlow_deleteCommentFromOneTicketDoesNotAffectOther() throws Exception {
        Ticket secondTicket = Ticket.builder()
                .title("Second Ticket")
                .status(TicketStatus.OPEN)
                .priority(Priority.LOW)
                .project(project)
                .assignee(user)
                .dueDate(OffsetDateTime.now().plusDays(5))
                .build();
        secondTicket = ticketRepository.save(secondTicket);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticket.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"Comment A\"}"));

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", secondTicket.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"Comment B\"}"));

        Long commentId = commentRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/comments/{commentId}",
                        ticket.getId(), commentId))
                .andExpect(status().isNoContent());

        assertThat(commentRepository.findAll()).hasSize(1);
        assertThat(commentRepository.findAll().get(0).getBody()).isEqualTo("Comment B");
    }

    @Test
    void errorFlow_getCommentsForNonExistentTicket_thenReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/{ticketId}/comments", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/tickets/99999/comments"));
    }

    @Test
    void errorFlow_addCommentToNonExistentTicket_thenReturns404() throws Exception {
        String body = """
                {
                    "body": "Comment for non-existent ticket"
                }
                """;

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket with id 99999 not found"));

        assertThat(commentRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_addCommentWithBlankBody_thenReturns400() throws Exception {
        String body = """
                {
                    "body": ""
                }
                """;

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("body")));

        assertThat(commentRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_addCommentWithMissingBody_thenReturns400() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("body")));

        assertThat(commentRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_addCommentWithMalformedJson_thenReturns400() throws Exception {
        String malformedJson = "{ \"body\": }";

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed Request"));

        assertThat(commentRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_deleteNonExistentComment_thenReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/comments/{commentId}",
                        ticket.getId(), 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Comment with id 99999 not found"));
    }
}
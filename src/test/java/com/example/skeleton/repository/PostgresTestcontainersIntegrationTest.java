package com.example.skeleton.repository;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Priority;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.enums.TicketStatus;
import com.example.skeleton.domain.model.Comment;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class PostgresTestcontainersIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void testUserProjectTicketCommentChain() {
        User user = User.builder()
                .email("integration.test@example.com")
                .passwordHash(null)
                .fullName("Integration Tester")
                .role(Role.PROJECT_MANAGER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();

        Project project = Project.builder()
                .name("Integration Project")
                .description("Project for integration test")
                .createdBy(savedUser)
                .build();
        Project savedProject = projectRepository.save(project);
        assertThat(savedProject.getId()).isNotNull();

        Ticket ticket = Ticket.builder()
                .title("Integration Ticket")
                .description("Ticket created during integration test")
                .status(TicketStatus.OPEN)
                .priority(Priority.HIGH)
                .project(savedProject)
                .assignee(savedUser)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);
        assertThat(savedTicket.getId()).isNotNull();

        Comment comment = Comment.builder()
                .ticket(savedTicket)
                .author(savedUser)
                .body("This is an integration test comment.")
                .build();
        Comment savedComment = commentRepository.save(comment);
        assertThat(savedComment.getId()).isNotNull();

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        Project foundProject = projectRepository.findById(savedProject.getId()).orElseThrow();
        Ticket foundTicket = ticketRepository.findById(savedTicket.getId()).orElseThrow();
        Comment foundComment = commentRepository.findById(savedComment.getId()).orElseThrow();

        assertThat(foundUser.getEmail()).isEqualTo("integration.test@example.com");
        assertThat(foundUser.getFullName()).isEqualTo("Integration Tester");

        assertThat(foundProject.getName()).isEqualTo("Integration Project");
        assertThat(foundProject.getCreatedBy().getId()).isEqualTo(savedUser.getId());

        assertThat(foundTicket.getTitle()).isEqualTo("Integration Ticket");
        assertThat(foundTicket.getProject().getId()).isEqualTo(savedProject.getId());
        assertThat(foundTicket.getAssignee().getId()).isEqualTo(savedUser.getId());

        assertThat(foundComment.getBody()).isEqualTo("This is an integration test comment.");
        assertThat(foundComment.getTicket().getId()).isEqualTo(savedTicket.getId());
        assertThat(foundComment.getAuthor().getId()).isEqualTo(savedUser.getId());

        Long projectCount = projectRepository.countByCreatedById(savedUser.getId());
        assertThat(projectCount).isEqualTo(1);

        Long ticketCount = ticketRepository.countByAssigneeId(savedUser.getId());
        assertThat(ticketCount).isEqualTo(1);

        Long commentCount = commentRepository.countByTicketId(savedTicket.getId());
        assertThat(commentCount).isEqualTo(1);
    }
}
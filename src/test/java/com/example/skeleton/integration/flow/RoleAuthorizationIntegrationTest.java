package com.example.skeleton.integration.flow;

import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.Ticket;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.repository.CommentRepository;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.RefreshTokenRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(properties = "security.dev-mode=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RoleAuthorizationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private String adminToken;
    private String pmToken;
    private String memberToken;

    private Long adminId;
    private Long pmId;
    private Long memberId;

    private Long projectId;
    private Long ticketAssignedToMember;
    private Long ticketAssignedToPm;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        registerUser("admin@test.com", "password123", "Admin");
        registerUser("pm@test.com", "password123", "PM");
        registerUser("member@test.com", "password123", "Member");

        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User pm = userRepository.findByEmail("pm@test.com").orElseThrow();
        pm.setRole(Role.PROJECT_MANAGER);
        userRepository.save(pm);

        User member = userRepository.findByEmail("member@test.com").orElseThrow();
        member.setRole(Role.MEMBER);
        userRepository.save(member);

        adminId = admin.getId();
        pmId = pm.getId();
        memberId = member.getId();

        adminToken = login("admin@test.com", "password123");
        pmToken = login("pm@test.com", "password123");
        memberToken = login("member@test.com", "password123");

        Project project = Project.builder()
                .name("US4 Project")
                .description("For role-based tests")
                .createdBy(admin)
                .build();
        project = projectRepository.save(project);
        projectId = project.getId();

        Ticket ticketForMember = Ticket.builder()
                .title("Ticket for Member")
                .status(com.example.skeleton.domain.enums.TicketStatus.OPEN)
                .priority(com.example.skeleton.domain.enums.Priority.LOW)
                .project(project)
                .assignee(member)
                .build();
        ticketForMember = ticketRepository.save(ticketForMember);
        ticketAssignedToMember = ticketForMember.getId();

        Ticket ticketForPm = Ticket.builder()
                .title("Ticket for PM")
                .status(com.example.skeleton.domain.enums.TicketStatus.OPEN)
                .priority(com.example.skeleton.domain.enums.Priority.LOW)
                .project(project)
                .assignee(pm)
                .build();
        ticketForPm = ticketRepository.save(ticketForPm);
        ticketAssignedToPm = ticketForPm.getId();
    }

    @Test
    void whenMemberDeletesProject_thenReturns403() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenProjectManagerDeletesProject_thenReturns403() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenAdminDeletesProjectWithTickets_thenReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Cannot delete project with existing tickets")));
    }

    @Test
    void whenAdminDeletesEmptyProject_thenReturns204() throws Exception {
        User admin = userRepository.findById(adminId).orElseThrow();
        Project empty = Project.builder()
                .name("Empty Project")
                .description("No tickets")
                .createdBy(admin)
                .build();
        empty = projectRepository.save(empty);

        mockMvc.perform(delete("/api/v1/projects/{id}", empty.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void whenMemberDeletesTicket_thenReturns403() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets/{id}", ticketAssignedToPm)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenProjectManagerDeletesTicket_thenReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets/{id}", ticketAssignedToPm)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void whenAdminDeletesTicket_thenReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tickets/{id}", ticketAssignedToMember)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void whenMemberPatchesOwnTicket_thenReturns200() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketAssignedToMember)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void whenMemberPatchesNotOwnTicket_thenReturns403() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketAssignedToPm)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenProjectManagerPatchesAnyTicket_thenReturns200() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketAssignedToMember)
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void whenAdminPatchesAnyTicket_thenReturns200() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketAssignedToPm)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void whenAnyRoleGetsTickets_thenReturns200() throws Exception {
        for (String token : new String[]{adminToken, pmToken, memberToken}) {
            mockMvc.perform(get("/api/v1/tickets")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void whenNoToken_thenProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenMemberCreatesTicket_thenReturns201() throws Exception {
        String body = String.format(
                "{\"title\":\"New ticket\",\"priority\":\"LOW\",\"projectId\":%d}",
                projectId);

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private void registerUser(String email, String password, String fullName) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\"}",
                                email, password, fullName)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"%s\"}",
                                email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }
}
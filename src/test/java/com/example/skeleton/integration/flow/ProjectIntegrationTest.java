package com.example.skeleton.integration.flow;

import com.example.skeleton.domain.enums.AuthProvider;
import com.example.skeleton.domain.enums.Role;
import com.example.skeleton.domain.model.Project;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ProjectIntegrationTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .email("project.tester@example.com")
                .fullName("Project Tester")
                .role(Role.PROJECT_MANAGER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        user = userRepository.save(user);
    }

    @Test
    void fullFlow_createGetDeleteProject() throws Exception {
        String createBody = """
                {
                    "name": "Integration Project",
                    "description": "Created via integration test",
                    "createdById": %d
                }
                """.formatted(user.getId());

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Project"))
                .andExpect(jsonPath("$.description").value("Created via integration test"))
                .andExpect(jsonPath("$.createdBy.id").value(user.getId()));

        List<Project> projects = projectRepository.findAll();
        assertThat(projects).hasSize(1);
        Long projectId = projects.get(0).getId();

        mockMvc.perform(get("/api/v1/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Integration Project"))
                .andExpect(jsonPath("$.createdBy.id").value(user.getId()));

        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(projectId))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(delete("/api/v1/projects/{id}", projectId))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(projectId)).isEmpty();
    }

    @Test
    void paginationFlow_multipleProjectsSortedAndPaginated() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Project p = Project.builder()
                    .name("Project " + i)
                    .description("Description " + i)
                    .createdBy(user)
                    .build();
            projectRepository.save(p);
        }

        assertThat(projectRepository.findAll()).hasSize(5);

        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));

        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    void paginationFlow_sortByNameAscending() throws Exception {
        createProjectDirectly("Zebra Project");
        createProjectDirectly("Alpha Project");
        createProjectDirectly("Mike Project");

        mockMvc.perform(get("/api/v1/projects")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Alpha Project"))
                .andExpect(jsonPath("$.content[1].name").value("Mike Project"))
                .andExpect(jsonPath("$.content[2].name").value("Zebra Project"));
    }

    @Test
    void paginationFlow_sortByNameDescending() throws Exception {
        createProjectDirectly("Zebra Project");
        createProjectDirectly("Alpha Project");
        createProjectDirectly("Mike Project");

        mockMvc.perform(get("/api/v1/projects")
                        .param("sortBy", "name")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Zebra Project"))
                .andExpect(jsonPath("$.content[1].name").value("Mike Project"))
                .andExpect(jsonPath("$.content[2].name").value("Alpha Project"));
    }

    @Test
    void errorFlow_getNonExistentProject_thenReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Project with id 99999 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/projects/99999"));
    }

    @Test
    void errorFlow_createProjectWithNonExistentUser_thenReturns404() throws Exception {
        String body = """
                {
                    "name": "Test Project",
                    "description": "Test",
                    "createdById": 99999
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with id 99999 not found"));

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_createProjectWithBlankName_thenReturns400() throws Exception {
        String body = """
                {
                    "name": "",
                    "description": "Test",
                    "createdById": %d
                }
                """.formatted(user.getId());

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("name")));

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_createProjectWithMissingName_thenReturns400() throws Exception {
        String body = """
                {
                    "description": "Test",
                    "createdById": %d
                }
                """.formatted(user.getId());

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("name")));

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_createProjectWithMissingCreatedById_thenReturns400() throws Exception {
        String body = """
                {
                    "name": "Test Project",
                    "description": "Test"
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("createdById")));

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    void errorFlow_createProjectWithMalformedJson_thenReturns400() throws Exception {
        String malformedJson = "{ \"name\": \"Test\", \"createdById\": }";

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed Request"));
    }

    @Test
    void errorFlow_deleteNonExistentProject_thenReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project with id 99999 not found"));
    }

    private Project createProjectDirectly(String name) {
        Project project = Project.builder()
                .name(name)
                .description("Description for " + name)
                .createdBy(user)
                .build();
        return projectRepository.save(project);
    }
}
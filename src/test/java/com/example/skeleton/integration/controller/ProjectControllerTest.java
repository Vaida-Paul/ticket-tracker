package com.example.skeleton.integration.controller;

import com.example.skeleton.config.SecurityProperties;
import com.example.skeleton.controller.ProjectController;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.openapitools.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUpSecurityProperties() {
        when(securityProperties.isDevMode()).thenReturn(true);
    }

    @BeforeEach
    void setUpSecurityContext() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("test@example.com")
                .claim("userId", 1L)
                .claim("role", "PROJECT_MANAGER")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_PROJECT_MANAGER")),
                "test@example.com"
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenGetProjects_thenReturns200AndPage() throws Exception {
        ProjectDto dto = new ProjectDto();
        dto.setId(1L);
        dto.setName("Test Project");
        dto.setDescription("Test Description");

        org.springframework.data.domain.Page<ProjectDto> mockPage =
                new org.springframework.data.domain.PageImpl<>(List.of(dto));

        when(projectService.getAllProjects(
                eq(0), eq(10), eq("createdAt"), eq("desc")
        )).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Test Project"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));

        verify(projectService).getAllProjects(
                eq(0), eq(10), eq("createdAt"), eq("desc")
        );
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenNoProjects_thenReturns200AndEmptyPage() throws Exception {
        org.springframework.data.domain.Page<ProjectDto> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());

        when(projectService.getAllProjects(
                eq(0), eq(20), eq("createdAt"), eq("desc")
        )).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/projects")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(projectService).getAllProjects(
                eq(0), eq(20), eq("createdAt"), eq("desc")
        );
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenGetProjectsWithSorting_thenReturns200() throws Exception {
        org.springframework.data.domain.Page<ProjectDto> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());

        when(projectService.getAllProjects(
                eq(0), eq(10), eq("name"), eq("asc")
        )).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(projectService).getAllProjects(
                eq(0), eq(10), eq("name"), eq("asc")
        );
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenProjectExists_thenReturns200AndProject() throws Exception {
        Long projectId = 1L;
        ProjectDto response = new ProjectDto();
        response.setId(projectId);
        response.setName("Test Project");
        response.setDescription("Test Description");

        when(projectService.getProjectById(projectId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.description").value("Test Description"));

        verify(projectService).getProjectById(projectId);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenProjectDoesNotExist_thenReturns404() throws Exception {
        Long projectId = 99999L;
        when(projectService.getProjectById(projectId))
                .thenThrow(new ProjectNotFoundException(projectId));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Project with id 99999 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/projects/99999"));

        verify(projectService).getProjectById(projectId);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenCreateProject_thenReturns201AndProject() throws Exception {
        String requestBody = """
            {
                "name": "New Project",
                "description": "New Description"
            }
            """;

        ProjectDto expectedResponse = new ProjectDto();
        expectedResponse.setId(1L);
        expectedResponse.setName("New Project");
        expectedResponse.setDescription("New Description");

        ArgumentCaptor<ProjectCreateRequest> captor = ArgumentCaptor.forClass(ProjectCreateRequest.class);
        when(projectService.createProject(captor.capture(), eq(1L)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New Project"))
                .andExpect(jsonPath("$.description").value("New Description"));

        verify(projectService).createProject(captor.capture(), eq(1L));

        ProjectCreateRequest captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("New Project");
        assertThat(captured.getDescription()).isEqualTo("New Description");

        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenCreateProjectWithBlankName_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "name": "",
                    "description": "Test",
                    "createdById": 1
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        verifyNoInteractions(projectService);
    }

    @Test
    void whenCreateProjectWithMissingName_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "description": "Test",
                    "createdById": 1
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        verifyNoInteractions(projectService);
    }

    @Test
    void whenCreateProjectWithNonExistentUser_thenReturns404() throws Exception {
        String requestBody = """
            {
                "name": "New Project",
                "description": "Test"
            }
            """;

        ArgumentCaptor<ProjectCreateRequest> captor = ArgumentCaptor.forClass(ProjectCreateRequest.class);
        when(projectService.createProject(captor.capture(), eq(1L)))
                .thenThrow(new UserNotFoundException(1L));

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with id 1 not found"));

        verify(projectService).createProject(captor.capture(), eq(1L));

        ProjectCreateRequest captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("New Project");

        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenDeleteProject_thenReturns204() throws Exception {
        Long projectId = 1L;
        doNothing().when(projectService).deleteProject(projectId);

        mockMvc.perform(delete("/api/v1/projects/{id}", projectId))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(projectId);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenDeleteNonExistentProject_thenReturns404() throws Exception {
        Long projectId = 99999L;
        doThrow(new ProjectNotFoundException(projectId))
                .when(projectService).deleteProject(projectId);

        mockMvc.perform(delete("/api/v1/projects/{id}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project with id 99999 not found"));

        verify(projectService).deleteProject(projectId);
        verifyNoMoreInteractions(projectService);
    }
}
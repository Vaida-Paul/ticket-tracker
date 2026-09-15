package com.example.skeleton.integration.controller;

import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.openapitools.model.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void whenGetProjects_thenReturns200AndPage() throws Exception {
        ProjectDto dto = new ProjectDto();
        dto.setId(1L);
        dto.setName("Test Project");
        dto.setDescription("Test Description");

        org.springframework.data.domain.Page<ProjectDto> mockPage =
                new org.springframework.data.domain.PageImpl<>(List.of(dto));

        when(projectService.getAllProjects(any(), any(), any(), any())).thenReturn(mockPage);

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

        verify(projectService).getAllProjects(any(), any(), any(), any());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenNoProjects_thenReturns200AndEmptyPage() throws Exception {
        org.springframework.data.domain.Page<ProjectDto> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());

        when(projectService.getAllProjects(any(), any(), any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/projects")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(projectService).getAllProjects(any(), any(), any(), any());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    void whenGetProjectsWithSorting_thenReturns200() throws Exception {
        org.springframework.data.domain.Page<ProjectDto> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());

        when(projectService.getAllProjects(any(), any(), any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(projectService).getAllProjects(any(), any(), any(), any());
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
                    "description": "New Description",
                    "createdById": 1
                }
                """;

        ProjectDto expectedResponse = new ProjectDto();
        expectedResponse.setId(1L);
        expectedResponse.setName("New Project");
        expectedResponse.setDescription("New Description");

        when(projectService.createProject(any(ProjectCreateRequest.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New Project"))
                .andExpect(jsonPath("$.description").value("New Description"));

        verify(projectService).createProject(any(ProjectCreateRequest.class));
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
    void whenCreateProjectWithMissingCreatedById_thenReturns400() throws Exception {
        String requestBody = """
                {
                    "name": "New Project",
                    "description": "Test"
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("createdById")));

        verifyNoInteractions(projectService);
    }

    @Test
    void whenCreateProjectWithNonExistentUser_thenReturns404() throws Exception {
        String requestBody = """
                {
                    "name": "New Project",
                    "description": "Test",
                    "createdById": 99999
                }
                """;

        when(projectService.createProject(any(ProjectCreateRequest.class)))
                .thenThrow(new UserNotFoundException(99999L));

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with id 99999 not found"));

        verify(projectService).createProject(any(ProjectCreateRequest.class));
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
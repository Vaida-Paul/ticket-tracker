package com.example.skeleton.unit.service;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.mapper.ProjectMapper;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.implementation.ProjectServiceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.ProjectCreateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImp projectService;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .build();
        user.setId(1L);

        project = Project.builder()
                .name("Test Project")
                .description("Description")
                .createdBy(user)
                .build();
        project.setId(1L);
    }

    @Test
    void whenGetAllProjects_thenReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> expectedPage = new PageImpl<>(List.of(project));
        when(projectRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Project> result = projectService.getAllProjects(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Project");

        verify(projectRepository).findAll(pageable);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper);
    }

    @Test
    void whenGetAllProjectsEmpty_thenReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> emptyPage = new PageImpl<>(List.of());
        when(projectRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<Project> result = projectService.getAllProjects(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(projectRepository).findAll(pageable);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper);
    }

    @Test
    void whenProjectExists_thenReturnsProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Project result = projectService.getProjectById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Project");

        verify(projectRepository).findById(1L);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper);
    }

    @Test
    void whenProjectDoesNotExist_thenThrowsException() {
        when(projectRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99999L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("99999");

        verify(projectRepository).findById(99999L);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper);
    }

    @Test
    void whenCreateProjectWithValidData_thenReturnsSavedProject() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("New Project");
        request.setCreatedById(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);

        Project result = projectService.createProject(request);

        assertThat(result).isNotNull();
        assertThat(result.getCreatedBy()).isEqualTo(user);

        verify(userRepository).findById(1L);
        verify(projectMapper).toEntity(request);
        verify(projectRepository).save(project);

        verifyNoMoreInteractions(userRepository, projectMapper, projectRepository);
    }

    @Test
    void whenCreateProjectWithNonExistentUser_thenThrowsException() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("New Project");
        request.setCreatedById(99999L);

        when(userRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99999");

        verify(userRepository).findById(99999L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(projectMapper, projectRepository);
    }

    @Test
    void whenDeleteProject_thenDeletesSuccessfully() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectRepository).delete(project);

        projectService.deleteProject(1L);

        verify(projectRepository).findById(1L);
        verify(projectRepository).delete(project);

        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper);
    }

    @Test
    void whenDeleteNonExistentProject_thenThrowsException() {
        when(projectRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99999L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("99999");

        verify(projectRepository).findById(99999L);
        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper);
    }
}
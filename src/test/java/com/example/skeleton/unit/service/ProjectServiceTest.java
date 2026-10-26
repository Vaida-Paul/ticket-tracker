package com.example.skeleton.unit.service;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.mapper.ProjectMapper;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.implementation.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.ProjectCreateRequest;
import org.openapitools.model.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    private TicketRepository ticketRepository;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private User user;
    private Project project;
    private ProjectDto projectDto;

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

        projectDto = new ProjectDto();
        projectDto.setId(1L);
        projectDto.setName("Test Project");
        projectDto.setDescription("Description");
    }


    @Test
    void whenGetAllProjects_thenReturnsPageOfDtos() {
        Page<Project> projectPage = new PageImpl<>(List.of(project));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(projectRepository.findAll(pageableCaptor.capture())).thenReturn(projectPage);
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        Page<ProjectDto> result = projectService.getAllProjects(0, 10, "createdAt", "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Project");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(projectRepository).findAll(pageableCaptor.capture());
        verify(projectMapper).toDto(project);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        Sort.Order order = capturedPageable.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);

        verifyNoMoreInteractions(projectRepository, projectMapper);
        verifyNoInteractions(userRepository, ticketRepository);
    }

    @Test
    void whenGetAllProjectsEmpty_thenReturnsEmptyPage() {
        Page<Project> emptyPage = new PageImpl<>(List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(projectRepository.findAll(pageableCaptor.capture())).thenReturn(emptyPage);

        Page<ProjectDto> result = projectService.getAllProjects(0, 10, "createdAt", "desc");

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(projectRepository).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        Sort.Order order = capturedPageable.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);

        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper, ticketRepository);
    }

    @Test
    void whenGetAllProjectsWithDefaultParams_thenUsesDefaults() {
        Page<Project> projectPage = new PageImpl<>(List.of(project));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(projectRepository.findAll(pageableCaptor.capture())).thenReturn(projectPage);
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        Page<ProjectDto> result = projectService.getAllProjects(null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(projectRepository).findAll(pageableCaptor.capture());
        verify(projectMapper).toDto(project);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(20);
        Sort.Order order = capturedPageable.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);

        verifyNoMoreInteractions(projectRepository, projectMapper);
        verifyNoInteractions(userRepository, ticketRepository);
    }

    @Test
    void whenProjectExists_thenReturnsProjectDto() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        ProjectDto result = projectService.getProjectById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Project");

        verify(projectRepository).findById(1L);
        verify(projectMapper).toDto(project);

        verifyNoMoreInteractions(projectRepository, projectMapper);
        verifyNoInteractions(userRepository, ticketRepository);
    }

    @Test
    void whenProjectDoesNotExist_thenThrowsException() {
        when(projectRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99999L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("99999");

        verify(projectRepository).findById(99999L);

        verifyNoMoreInteractions(projectRepository);
        verifyNoInteractions(userRepository, projectMapper, ticketRepository);
    }

    @Test
    void whenCreateProjectWithValidData_thenReturnsProjectDto() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("New Project");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        ProjectDto result = projectService.createProject(request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Project");
        assertThat(project.getCreatedBy()).isEqualTo(user);

        verify(userRepository).findById(1L);
        verify(projectMapper).toEntity(request);
        verify(projectRepository).save(project);
        verify(projectMapper).toDto(project);

        verifyNoMoreInteractions(userRepository, projectMapper, projectRepository);
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void whenCreateProjectWithNonExistentUser_thenThrowsException() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("New Project");

        when(userRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(request, 99999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99999");

        verify(userRepository).findById(99999L);

        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(projectMapper, projectRepository, ticketRepository);
    }

    @Test
    void whenDeleteProject_thenDeletesSuccessfully() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(ticketRepository.existsByProjectId(1L)).thenReturn(false);
        doNothing().when(projectRepository).delete(project);

        projectService.deleteProject(1L);

        verify(projectRepository).findById(1L);
        verify(ticketRepository).existsByProjectId(1L);
        verify(projectRepository).delete(project);

        verifyNoMoreInteractions(projectRepository, ticketRepository);
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
        verifyNoInteractions(userRepository, projectMapper, ticketRepository);
    }
}
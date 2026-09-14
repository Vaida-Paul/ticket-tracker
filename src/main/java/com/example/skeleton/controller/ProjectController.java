package com.example.skeleton.controller;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.mapper.ProjectMapper;
import com.example.skeleton.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.ProjectsApi;
import org.openapitools.model.PageResponseProjectDto;
import org.openapitools.model.ProjectCreateRequest;
import org.openapitools.model.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectController implements ProjectsApi {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    @Override
    public ResponseEntity<PageResponseProjectDto> getProjects(
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        String sortField = sortBy != null && !sortBy.isEmpty() ? sortBy : "createdAt";
        Sort.Direction direction = Sort.Direction.fromString(
                sortDirection != null && !sortDirection.isEmpty() ? sortDirection : "desc"
        );
        Sort sortObj = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                sortObj
        );
        Page<Project> projectPage = projectService.getAllProjects(pageable);
        PageResponseProjectDto response = new PageResponseProjectDto();
        response.setContent(projectPage.getContent().stream()
                .map(projectMapper::toDto)
                .toList());
        response.setTotalElements(projectPage.getTotalElements());
        response.setTotalPages(projectPage.getTotalPages());
        response.setSize(projectPage.getSize());
        response.setPage(projectPage.getNumber());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProjectDto> getProjectById(Long id) {
        Project project = projectService.getProjectById(id);
        return ResponseEntity.ok(projectMapper.toDto(project));
    }

    @Override
    public ResponseEntity<ProjectDto> createProject(@Valid ProjectCreateRequest request) {
        Project project = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMapper.toDto(project));
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}

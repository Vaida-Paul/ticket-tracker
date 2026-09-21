package com.example.skeleton.controller;

import com.example.skeleton.security.SecurityUtils;
import com.example.skeleton.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.ProjectsApi;
import org.openapitools.model.PageResponseProjectDto;
import org.openapitools.model.ProjectCreateRequest;
import org.openapitools.model.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProjectController implements ProjectsApi {

    private final ProjectService projectService;

    @Override
    public ResponseEntity<PageResponseProjectDto> getProjects(
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        Page<ProjectDto> projectPage = projectService.getAllProjects(page, size, sortBy, sortDirection);

        PageResponseProjectDto response = new PageResponseProjectDto();
        response.setContent(projectPage.getContent());
        response.setTotalElements(projectPage.getTotalElements());
        response.setTotalPages(projectPage.getTotalPages());
        response.setSize(projectPage.getSize());
        response.setPage(projectPage.getNumber());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProjectDto> getProjectById(Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @Override
    public ResponseEntity<ProjectDto> createProject(@Valid ProjectCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectDto project = projectService.createProject(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
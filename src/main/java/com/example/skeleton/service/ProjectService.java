package com.example.skeleton.service;

import com.example.skeleton.domain.model.Project;
import org.openapitools.model.ProjectCreateRequest;
import org.openapitools.model.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    /**
     * Retrieves all projects with pagination and sorting.
     *
     * @param page          the page number to retrieve (0-based)
     * @param size          the number of projects per page
     * @param sortBy        the field used for sorting the results
     * @param sortDirection the sorting direction (asc or desc)
     * @return a page of matching project DTOs
     */
    Page<ProjectDto> getAllProjects(Integer page, Integer size, String sortBy, String sortDirection);

    /**
     * Retrieves a project by its ID.
     *
     * @param id the ID of the project
     * @return the project DTO
     * @throws com.example.skeleton.exception.ProjectNotFoundException if the project does not exist
     */
    ProjectDto getProjectById(Long id);

    /**
     * Creates a new project.
     *
     * @param request the project data (name, description, createdById)
     * @return the created project DTO
     * @throws com.example.skeleton.exception.UserNotFoundException if the creator user does not exist
     */
    ProjectDto createProject(ProjectCreateRequest request);

    /**
     * Deletes a project by its ID.
     *
     * @param id the ID of the project to delete
     * @throws com.example.skeleton.exception.ProjectNotFoundException if the project does not exist
     */
    void deleteProject(Long id);
}
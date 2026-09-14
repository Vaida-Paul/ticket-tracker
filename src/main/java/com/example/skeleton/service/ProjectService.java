package com.example.skeleton.service;

import com.example.skeleton.domain.model.Project;
import org.openapitools.model.ProjectCreateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    Page<Project> getAllProjects(Pageable pageable);
    Project getProjectById(Long id);
    Project createProject(ProjectCreateRequest request);
    void deleteProject(Long id);
}
package com.example.skeleton.service.implementation;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.mapper.ProjectMapper;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.ProjectCreateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImp implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<Project> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    @Override
    @Transactional
    public Project createProject(ProjectCreateRequest request) {
        User createdBy = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new UserNotFoundException(request.getCreatedById()));

        Project project = projectMapper.toEntity(request);
        project.setCreatedBy(createdBy);
        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = getProjectById(id);
        projectRepository.delete(project);
    }
}
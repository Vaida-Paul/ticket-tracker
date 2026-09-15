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
import org.openapitools.model.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImp implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectDto> getAllProjects(Integer page, Integer size, String sortBy, String sortDirection) {
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

        return projectRepository.findAll(pageable).map(projectMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return projectMapper.toDto(project);
    }

    @Override
    @Transactional
    public ProjectDto createProject(ProjectCreateRequest request) {
        log.info("Creating project: name={}, createdById={}",
                request.getName(), request.getCreatedById());

        User createdBy = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new UserNotFoundException(request.getCreatedById()));

        Project project = projectMapper.toEntity(request);
        project.setCreatedBy(createdBy);

        Project saved = projectRepository.save(project);
        return projectMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        projectRepository.delete(project);
    }
}
package com.example.skeleton.service.implementation;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.User;
import com.example.skeleton.exception.InvalidRequestException;
import com.example.skeleton.exception.ProjectNotFoundException;
import com.example.skeleton.exception.UserNotFoundException;
import com.example.skeleton.mapper.ProjectMapper;
import com.example.skeleton.repository.ProjectRepository;
import com.example.skeleton.repository.TicketRepository;
import com.example.skeleton.repository.UserRepository;
import com.example.skeleton.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.ProjectCreateRequest;
import org.openapitools.model.ProjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.example.skeleton.utils.RequestValidationUtils.*;

@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final ProjectMapper projectMapper;

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "name");

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectDto> getAllProjects(Integer page, Integer size, String sortBy, String sortDirection) {
        Pageable pageable = buildPageable(page, size, sortBy, "createdAt", sortDirection, SORT_FIELDS);
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
        log.info("Deleting project id: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        if (ticketRepository.existsByProjectId(id)) {
            throw new InvalidRequestException(
                    "Cannot delete project with existing tickets. Delete or reassign tickets first.");
        }

        projectRepository.delete(project);
    }
}
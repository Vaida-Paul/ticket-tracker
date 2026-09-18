package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.model.ProjectCreateRequest;
import org.openapitools.model.ProjectDto;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectDto toDto(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    Project toEntity(ProjectCreateRequest request);
}

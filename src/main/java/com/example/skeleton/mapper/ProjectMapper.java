package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.model.ProjectCreateRequest;
import org.openapitools.model.ProjectDto;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProjectMapper {

    @Mapping(source = "createdBy.id", target = "createdBy.id")
    @Mapping(source = "createdBy.email", target = "createdBy.email")
    @Mapping(source = "createdBy.fullName", target = "createdBy.fullName")
    ProjectDto toDto(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    Project toEntity(ProjectCreateRequest request);
}

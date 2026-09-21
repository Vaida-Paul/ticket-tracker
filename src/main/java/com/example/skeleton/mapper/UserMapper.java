package com.example.skeleton.mapper;

import com.example.skeleton.domain.model.User;
import org.mapstruct.Mapper;
import org.openapitools.model.UserDto;

@Mapper(componentModel = "spring", uses = EnumMapper.class)
public interface UserMapper {

    UserDto toDto(User user);
}

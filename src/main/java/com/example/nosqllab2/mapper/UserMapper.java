package com.example.nosqllab2.mapper;

import com.example.nosqllab2.dto.ref.RoleRefDto;
import com.example.nosqllab2.dto.response.UserResponseDto;
import com.example.nosqllab2.entity.Role;
import com.example.nosqllab2.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {
    UserResponseDto toDto(User user);

    RoleRefDto toRefDto(Role role);
}

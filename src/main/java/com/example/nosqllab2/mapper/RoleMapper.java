package com.example.nosqllab2.mapper;

import com.example.nosqllab2.dto.response.RoleResponseDto;
import com.example.nosqllab2.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapper {
    RoleResponseDto toDto(Role role);
}

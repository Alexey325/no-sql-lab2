package com.example.nosqllab2.dto.response;

import com.example.nosqllab2.dto.ref.RoleRefDto;

import java.util.Set;

public record UserResponseDto(
        Long id,
        String username,
        Set<RoleRefDto> roles
) {
}

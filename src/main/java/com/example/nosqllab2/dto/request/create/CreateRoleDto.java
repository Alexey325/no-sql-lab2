package com.example.nosqllab2.dto.request.create;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleDto(
        @NotBlank String value,
        @NotBlank String description
) {
}

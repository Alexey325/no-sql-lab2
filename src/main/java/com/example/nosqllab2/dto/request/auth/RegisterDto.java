package com.example.nosqllab2.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record RegisterDto(
        @NotBlank String username,
        @NotBlank String password
) {
}

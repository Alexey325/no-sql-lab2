package com.example.nosqllab2.dto.request.create;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryDto(
        @NotBlank String name
) {
}

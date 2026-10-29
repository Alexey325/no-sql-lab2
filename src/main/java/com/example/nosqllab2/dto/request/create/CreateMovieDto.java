package com.example.nosqllab2.dto.request.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateMovieDto(
        @NotBlank String title,
        @NotBlank String description,

        // название категории, а не id — так же, как в лабораторной №1
        @NotBlank String category,

        // дата и время показа в формате DD.MM HH:mm
        @NotBlank String showDate,

        @Positive int duration
) {
}

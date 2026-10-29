package com.example.nosqllab2.dto.request.update;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UpdateMovieDto(
        // новые дата и время показа в формате DD.MM HH:mm
        @NotBlank String showDate,

        // если не передана, остаётся прежней
        @Positive Integer duration
) {
}

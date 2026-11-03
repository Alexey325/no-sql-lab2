package com.example.nosqllab2.dto.response;

import com.example.nosqllab2.document.MovieState;

import java.time.LocalDateTime;

public record MovieStageDto(
        MovieState stage,
        LocalDateTime at
) {
}

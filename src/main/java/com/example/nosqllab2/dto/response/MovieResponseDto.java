package com.example.nosqllab2.dto.response;

import com.example.nosqllab2.document.MovieState;
import com.example.nosqllab2.dto.ref.CategoryRefDto;

import java.time.LocalDateTime;
import java.util.List;

public record MovieResponseDto(
        String id,
        String title,
        String description,
        LocalDateTime showDate,
        int duration,
        CategoryRefDto category,
        MovieState state,
        List<MovieStageDto> stages
) {
}

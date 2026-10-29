package com.example.nosqllab2.dto.response;

import com.example.nosqllab2.dto.ref.MovieRefDto;

import java.util.List;

public record CategoryWithMoviesDto(
        Long id,
        String name,
        List<MovieRefDto> movies
) {
}

package com.example.nosqllab2.dto.response.schedule;

import com.example.nosqllab2.dto.response.MovieResponseDto;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public record MovieScheduleDto(List<MovieResponseDto> movies) implements ScheduleResponseDto {

    // @JsonValue, чтобы в ответе был чистый массив фильмов, а не объект-обёртка
    @JsonValue
    @Override
    public List<MovieResponseDto> movies() {
        return movies;
    }
}

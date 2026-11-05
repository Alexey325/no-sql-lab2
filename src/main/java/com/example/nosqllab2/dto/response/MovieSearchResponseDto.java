package com.example.nosqllab2.dto.response;

import java.util.List;

public record MovieSearchResponseDto(
        List<MovieResponseDto> items,
        String nextCursor
) {
}

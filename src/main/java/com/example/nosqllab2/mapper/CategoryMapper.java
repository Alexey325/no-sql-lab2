package com.example.nosqllab2.mapper;

import com.example.nosqllab2.document.CategoryDocument;
import com.example.nosqllab2.document.MovieCopy;
import com.example.nosqllab2.dto.ref.MovieRefDto;
import com.example.nosqllab2.dto.response.CategoryResponseDto;
import com.example.nosqllab2.dto.response.CategoryWithMoviesDto;
import com.example.nosqllab2.util.ShowDateParser;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryMapper {

    CategoryResponseDto toDto(CategoryDocument category);

    CategoryWithMoviesDto toDtoWithMovies(CategoryDocument category);

    MovieRefDto toRefDto(String id, MovieCopy copy);

    // копии лежат в map, где ключ — id фильма
    default List<MovieRefDto> toMovieRefs(Map<String, MovieCopy> movies) {
        return movies.entrySet().stream()
                .map(entry -> toRefDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MovieRefDto::showDate))
                .toList();
    }

    default LocalDateTime toShowDate(String value) {
        return ShowDateParser.fromStoredValue(value);
    }
}

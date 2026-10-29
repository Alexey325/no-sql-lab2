package com.example.nosqllab2.mapper;

import com.example.nosqllab2.dto.ref.MovieRefDto;
import com.example.nosqllab2.dto.response.CategoryResponseDto;
import com.example.nosqllab2.dto.response.CategoryWithMoviesDto;
import com.example.nosqllab2.entity.Category;
import com.example.nosqllab2.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryMapper {
    CategoryResponseDto toDto(Category category);

    CategoryWithMoviesDto toDtoWithMovies(Category category);

    MovieRefDto toRefDto(Movie movie);
}

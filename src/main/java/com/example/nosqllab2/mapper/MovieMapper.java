package com.example.nosqllab2.mapper;

import com.example.nosqllab2.dto.ref.CategoryRefDto;
import com.example.nosqllab2.dto.response.MovieResponseDto;
import com.example.nosqllab2.entity.Category;
import com.example.nosqllab2.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MovieMapper {
    MovieResponseDto toDto(Movie movie);

    CategoryRefDto toRefDto(Category category);
}

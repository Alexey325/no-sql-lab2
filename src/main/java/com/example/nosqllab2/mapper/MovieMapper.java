package com.example.nosqllab2.mapper;

import com.example.nosqllab2.dto.response.MovieResponseDto;
import com.example.nosqllab2.document.MovieDocument;
import com.example.nosqllab2.util.ShowDateParser;
import com.google.cloud.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MovieMapper {

    // категория в документе фильма денормализована в два поля
    @Mapping(target = "category.id", source = "categoryId")
    @Mapping(target = "category.name", source = "categoryName")
    MovieResponseDto toDto(MovieDocument movie);

    default LocalDateTime toDateTime(Timestamp value) {
        return ShowDateParser.fromStoredValue(value);
    }
}

package com.example.nosqllab2.controller;

import com.example.nosqllab2.document.MovieState;
import com.example.nosqllab2.dto.request.create.CreateMovieDto;
import com.example.nosqllab2.dto.request.update.UpdateMovieDto;
import com.example.nosqllab2.dto.response.MovieResponseDto;
import com.example.nosqllab2.dto.response.MovieSearchResponseDto;
import com.example.nosqllab2.dto.response.MovieStatsDto;
import com.example.nosqllab2.dto.response.schedule.ScheduleResponseDto;
import com.example.nosqllab2.service.MovieService;
import com.example.nosqllab2.service.MovieStatsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private final MovieStatsService movieStatsService;

    public MovieController(MovieService movieService, MovieStatsService movieStatsService) {
        this.movieService = movieService;
        this.movieStatsService = movieStatsService;
    }

    // отдаёт либо расписание, либо уведомление о том, что в расписании ведутся правки
    @GetMapping
    public ResponseEntity<ScheduleResponseDto> getAllMovies() {
        return ResponseEntity.ok(movieService.findAllMovies());
    }

    @GetMapping("/stats")
    public ResponseEntity<MovieStatsDto> getStats() {
        return ResponseEntity.ok(movieStatsService.getStats());
    }

    @GetMapping("/search")
    public ResponseEntity<MovieSearchResponseDto> search(
            @RequestParam(required = false) List<MovieState> state,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String order,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(movieService.search(state, order, limit, cursor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(movieService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDto> create(@Valid @RequestBody CreateMovieDto dto) {
        MovieResponseDto created = movieService.addMovie(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDto> update(@PathVariable String id, @Valid @RequestBody UpdateMovieDto dto) {
        return ResponseEntity.ok(movieService.updateMovie(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}

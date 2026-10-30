package com.example.nosqllab2.controller;

import com.example.nosqllab2.dto.request.create.CreateMovieDto;
import com.example.nosqllab2.dto.request.update.UpdateMovieDto;
import com.example.nosqllab2.dto.response.MovieResponseDto;
import com.example.nosqllab2.dto.response.schedule.ScheduleResponseDto;
import com.example.nosqllab2.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // отдаёт либо расписание, либо уведомление о том, что в расписании ведутся правки
    @GetMapping
    public ResponseEntity<ScheduleResponseDto> getAllMovies() {
        return ResponseEntity.ok(movieService.findAllMovies());
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

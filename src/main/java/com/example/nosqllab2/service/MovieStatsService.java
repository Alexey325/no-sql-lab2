package com.example.nosqllab2.service;

import com.example.nosqllab2.document.MovieState;
import com.example.nosqllab2.dto.response.MovieStatsDto;
import com.example.nosqllab2.repository.firestore.MovieStatsRepository;
import com.google.cloud.firestore.UpdateBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MovieStatsService {

    private final MovieStatsRepository statsRepository;

    public MovieStatsService(MovieStatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public MovieStatsDto getStats() {
        Map<MovieState, Long> counts = statsRepository.read();

        return new MovieStatsDto(
                counts.get(MovieState.PLANNED),
                counts.get(MovieState.RESCHEDULED),
                counts.get(MovieState.SHOWN)
        );
    }

    public void recordCreated(UpdateBuilder<?> writes, MovieState state) {
        statsRepository.increment(writes, Map.of(state, 1L));
    }

    public void recordTransition(UpdateBuilder<?> writes, MovieState from, MovieState to) {
        if (from != to) {
            statsRepository.increment(writes, Map.of(from, -1L, to, 1L));
        }
    }

    public void recordDeleted(UpdateBuilder<?> writes, MovieState state) {
        statsRepository.increment(writes, Map.of(state, -1L));
    }
}

package com.example.nosqllab2.util;

import com.example.nosqllab2.document.MovieDocument;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class MovieScheduleUtils {

    private MovieScheduleUtils() {
    }

    public static DayBounds getDayBounds(LocalDateTime showDate) {
        return new DayBounds(
                showDate.toLocalDate().atStartOfDay(),
                showDate.toLocalDate().atTime(LocalTime.MAX)
        );
    }

    // в кинотеатре один зал, поэтому новый показ не должен пересекаться по времени с уже запланированными
    public static boolean hasTimeConflict(LocalDateTime newShowDate, int newDuration, List<MovieDocument> movies) {
        LocalDateTime newEnd = newShowDate.plusMinutes(newDuration);

        return movies.stream().anyMatch(movie -> {
            LocalDateTime existingStart = ShowDateParser.fromStoredValue(movie.getShowDate());
            LocalDateTime existingEnd = existingStart.plusMinutes(movie.getDuration());

            return newShowDate.isBefore(existingEnd) && newEnd.isAfter(existingStart);
        });
    }

    public record DayBounds(LocalDateTime startOfDay, LocalDateTime endOfDay) {
    }
}

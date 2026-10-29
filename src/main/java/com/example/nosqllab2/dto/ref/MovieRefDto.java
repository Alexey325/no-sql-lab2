package com.example.nosqllab2.dto.ref;

import java.time.LocalDateTime;

public record MovieRefDto(
        Long id,
        String title,
        String description,
        LocalDateTime showDate,
        int duration
) {
}

package com.example.nosqllab2.dto.ref;

import java.time.LocalDateTime;

public record MovieRefDto(
        String id,
        String title,
        String description,
        LocalDateTime showDate,
        int duration
) {
}

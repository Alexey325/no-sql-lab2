package com.example.nosqllab2.dto.response;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        String title,
        String message,
        LocalDateTime createdAt
) {
}

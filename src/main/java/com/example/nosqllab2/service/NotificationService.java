package com.example.nosqllab2.service;

import com.example.nosqllab2.dto.response.NotificationResponseDto;
import com.example.nosqllab2.util.ShowDateParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

// уведомления живут только в redis: создаются за 3 часа до показа, TTL до его начала
@Service
public class NotificationService {

    private static final String NOTIFICATION_KEY_PREFIX = "notification:";
    private static final Duration NOTIFICATION_LEAD_TIME = Duration.ofHours(3);

    private final AtomicLong nextId = new AtomicLong(1);

    private final RedisService redisService;
    private final RedisLockService redisLockService;
    private final TaskScheduler notificationTaskScheduler;
    private final ObjectMapper objectMapper;
    private final String nodeId;

    public NotificationService(
            RedisService redisService,
            RedisLockService redisLockService,
            TaskScheduler notificationTaskScheduler,
            ObjectMapper objectMapper,
            @Value("${app.node.id}") String nodeId
    ) {
        this.redisService = redisService;
        this.redisLockService = redisLockService;
        this.notificationTaskScheduler = notificationTaskScheduler;
        this.objectMapper = objectMapper;
        this.nodeId = nodeId;
    }

    public void scheduleNotification(String title, String message, LocalDateTime showDate) {
        Instant showInstant = showDate.atZone(ShowDateParser.ZONE).toInstant();
        Instant notificationInstant = showInstant.minus(NOTIFICATION_LEAD_TIME);

        if (!notificationInstant.isAfter(Instant.now())) {
            // до показа меньше 3 часов — создаём сразу
            if (showInstant.isAfter(Instant.now())) {
                createNotification(title, message, showInstant);
            }

            return;
        }

        notificationTaskScheduler.schedule(() -> createNotification(title, message, showInstant), notificationInstant);
    }

    public List<NotificationResponseDto> findAllNotifications() {
        Set<String> keys = redisService.keys(NOTIFICATION_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<NotificationResponseDto> notifications = new ArrayList<>();

        for (String key : keys) {
            String value = redisService.get(key);

            if (value == null) {
                continue;
            }

            try {
                notifications.add(objectMapper.readValue(value, NotificationResponseDto.class));
            } catch (JacksonException ex) {
                // битое значение пропускаем
            }
        }

        return notifications;
    }

    private void createNotification(String title, String message, Instant showInstant) {
        Duration ttl = Duration.between(Instant.now(), showInstant);

        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        NotificationResponseDto notification = new NotificationResponseDto(
                nextId.getAndIncrement(),
                title,
                message,
                ShowDateParser.now()
        );

        // node-id в ключе, иначе узлы перезапишут уведомления друг друга
        String key = NOTIFICATION_KEY_PREFIX + nodeId + ":" + notification.id();

        String payload;

        try {
            payload = objectMapper.writeValueAsString(notification);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось сериализовать уведомление", ex);
        }

        redisService.set(key, payload, ttl);
        redisLockService.broadcastKey(key, payload, ttl);
    }
}

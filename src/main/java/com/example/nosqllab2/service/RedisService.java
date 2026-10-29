package com.example.nosqllab2.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Работа с "своим" (локальным для этого узла) redis.
 * Под StringRedisTemplate лежит Jedis с пулом соединений на REDIS_POOL_SIZE штук:
 * каждая команда берёт свободное соединение из пула и возвращает его обратно,
 * поэтому подвисшая команда не блокирует остальные запросы приложения.
 */
@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            redisTemplate.opsForValue().set(key, value);
            return;
        }

        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // keys оставлен в рамках лабы, но операция тяжелая, можно оптимизировать используя scan
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}

package com.example.nosqllab2.service;

import com.example.nosqllab2.exception.custom.ScheduleLockedException;
import jakarta.annotation.PreDestroy;
import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisLockService {

    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final String EDITING_FLAG_PREFIX = "editing:";

    private final Map<String, RedissonClient> nodes = new LinkedHashMap<>();
    private final List<RedissonClient> broadcastTargets = new ArrayList<>();

    public RedisLockService(
            @Value("${app.redis.nodes}") String nodeAddresses,
            @Value("${spring.data.redis.host}") String localHost,
            @Value("${spring.data.redis.port}") int localPort
    ) {
        for (String address : nodeAddresses.split(",")) {
            String node = address.trim();

            if (node.isEmpty()) {
                continue;
            }

            nodes.put(node, createClient(node));
        }

        String selfAddress = localHost + ":" + localPort;

        nodes.forEach((address, client) -> {
            if (!address.equals(selfAddress)) {
                broadcastTargets.add(client);
            }
        });
    }

    public ScheduleLock acquireScheduleLock(List<String> dateKeys, Duration ttl) {
        List<RLock> acquiredLocks = new ArrayList<>();

        // сортировка дат, чтобы параллельные запросы не встали в клинч
        for (String dateKey : dateKeys.stream().distinct().sorted().toList()) {
            RLock redLock = redLockFor(dateKey);

            if (!tryLock(redLock, ttl)) {
                release(new ScheduleLock(acquiredLocks));
                throw new ScheduleLockedException();
            }

            acquiredLocks.add(redLock);
        }

        return new ScheduleLock(acquiredLocks);
    }

    public void release(ScheduleLock scheduleLock) {
        for (RLock lock : scheduleLock.locks()) {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void broadcastEditingFlag(List<String> dateKeys, Duration ttl) {
        for (String dateKey : dateKeys.stream().distinct().toList()) {
            broadcastKey(EDITING_FLAG_PREFIX + dateKey, "1", ttl);
        }
    }

    public void broadcastKey(String key, String value, Duration ttl) {
        for (RedissonClient target : broadcastTargets) {
            try {
                target.getBucket(key, StringCodec.INSTANCE).set(value, ttl);
            } catch (RuntimeException ex) {

            }
        }
    }

    @PreDestroy
    public void shutdown() {
        nodes.values().forEach(RedissonClient::shutdown);
    }

    // RedLock, в отличие от getMultiLock, требует большинства узлов, а не всех
    @SuppressWarnings("deprecation")
    private RLock redLockFor(String dateKey) {
        RLock[] locks = nodes.values().stream()
                .map(client -> client.getLock(LOCK_KEY_PREFIX + dateKey))
                .toArray(RLock[]::new);

        return new RedissonRedLock(locks);
    }

    private boolean tryLock(RLock redLock, Duration ttl) {
        try {
            return redLock.tryLock(0, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ScheduleLockedException();
        }
    }

    private static RedissonClient createClient(String address) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + address)
                .setConnectTimeout(5000)
                .setRetryAttempts(3)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(4);

        return Redisson.create(config);
    }

    public record ScheduleLock(List<RLock> locks) {
    }
}

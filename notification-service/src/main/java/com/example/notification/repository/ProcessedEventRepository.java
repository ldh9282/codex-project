package com.example.notification.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class ProcessedEventRepository {

    private static final String KEY_PREFIX = "notification:processed:event:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public ProcessedEventRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean reserveIfAbsent(String eventId) {
        // SETNX 기반으로 eventId를 선점하면 멀티 인스턴스/리밸런싱 상황에서도 중복 소비를 방지할 수 있다.
        Boolean success = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
        return Boolean.TRUE.equals(success);
    }
}

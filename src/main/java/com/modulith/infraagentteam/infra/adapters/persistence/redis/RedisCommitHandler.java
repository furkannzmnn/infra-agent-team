package com.modulith.infraagentteam.infra.adapters.persistence.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisCommitHandler {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String COMMIT_KEY_PREFIX = "commit:";
    private static final long COMMIT_EXPIRY_DAYS = 7;

    public boolean isCommitProcessed(String commitId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COMMIT_KEY_PREFIX + commitId));
    }

    public void markCommitAsProcessed(String commitId) {
        redisTemplate.opsForValue().set(
            COMMIT_KEY_PREFIX + commitId,
            "processed",
            COMMIT_EXPIRY_DAYS,
            TimeUnit.DAYS
        );
    }
} 
package com.example.skeleton.job;

import com.example.skeleton.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {
    private final RefreshTokenRepository refreshTokenRepository;

    public void cleanupExpiredAndRevoked() {
        log.info("Starting refresh token cleanup");
        try {
            int deleted = refreshTokenRepository.deleteExpiredAndRevoked(OffsetDateTime.now());
            log.info("Refresh token cleanup done: {} tokens deleted", deleted);
        } catch (Exception e) {
            log.error("Refresh token cleanup failed", e);
        }
    }
}
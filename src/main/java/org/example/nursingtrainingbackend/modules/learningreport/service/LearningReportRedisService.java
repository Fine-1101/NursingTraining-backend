package org.example.nursingtrainingbackend.modules.learningreport.service;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.aop.RateLimitService;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 学习报告 Redis 幂等、限流与缓存服务。
 */
@Service
@RequiredArgsConstructor
public class LearningReportRedisService {

    private static final String IDEMPOTENCY_PREFIX = "ai:report:idempotency:";
    private static final String DAILY_LIMIT_PREFIX = "ai:report:daily-limit:";
    private static final String REGENERATE_PREFIX = "ai:report:regenerate:";
    private static final String DETAIL_PREFIX = "ai:report:detail:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;

    public Optional<Long> findIdempotentReportId(
            Long userId,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        String redisKey = buildIdempotencyKey(userId, idempotencyKey);
        String value = redisTemplate.opsForValue().get(redisKey);

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException exception) {
            redisTemplate.delete(redisKey);
            return Optional.empty();
        }
    }

    public void saveIdempotentResult(
            Long userId,
            String idempotencyKey,
            Long reportId
    ) {
        redisTemplate.opsForValue().set(
                buildIdempotencyKey(userId, idempotencyKey),
                String.valueOf(reportId),
                Duration.ofHours(24)
        );
    }

    public void deleteIdempotentResult(
            Long userId,
            String idempotencyKey
    ) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            redisTemplate.delete(buildIdempotencyKey(userId, idempotencyKey));
        }
    }

    public void checkDailyLimit(Long userId, int limit) {
        String key = DAILY_LIMIT_PREFIX + userId + ":" + LocalDate.now();

        if (!rateLimitService.tryAcquire(key, limit, secondsUntilTomorrow())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_RATE_LIMITED,
                    "今日学习报告生成次数已达到上限"
            );
        }
    }

    public void checkRegenerateLimit(
            Long userId,
            Long reportId,
            int limit
    ) {
        String key = REGENERATE_PREFIX
                + userId + ":" + reportId + ":" + LocalDate.now();

        if (!rateLimitService.tryAcquire(key, limit, secondsUntilTomorrow())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_RATE_LIMITED,
                    "今日重新生成该报告的次数已达到上限"
            );
        }
    }

    public String getCachedReportDetail(Long reportId) {
        return redisTemplate.opsForValue().get(DETAIL_PREFIX + reportId);
    }

    public void cacheReportDetail(Long reportId, String reportJson) {
        redisTemplate.opsForValue().set(
                DETAIL_PREFIX + reportId,
                reportJson,
                Duration.ofHours(6)
        );
    }

    public void evictReportDetail(Long reportId) {
        redisTemplate.delete(DETAIL_PREFIX + reportId);
    }

    private String buildIdempotencyKey(Long userId, String idempotencyKey) {
        return IDEMPOTENCY_PREFIX + userId + ":" + idempotencyKey;
    }

    private int secondsUntilTomorrow() {
        long seconds = Duration.between(
                LocalDateTime.now(),
                LocalDate.now().plusDays(1).atStartOfDay()
        ).toSeconds();

        return Math.max(1, Math.toIntExact(seconds));
    }
}

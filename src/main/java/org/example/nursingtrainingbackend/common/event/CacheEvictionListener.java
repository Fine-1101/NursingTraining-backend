package org.example.nursingtrainingbackend.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionListener {

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-double-delete");
        t.setDaemon(true);
        return t;
    });

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCacheEviction(CacheEvictionEvent event) {
        doEvict(event.getScope());

        if (event.isDoubleDelete()) {
            scheduler.schedule(() -> {
                try {
                    doEvict(event.getScope());
                    log.debug("双删第二次执行完成, scope={}", event.getScope());
                } catch (Exception e) {
                    log.warn("双删第二次执行失败, scope={}", event.getScope(), e);
                }
            }, event.getDelayMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void doEvict(CacheEvictionEvent.Scope scope) {
        switch (scope) {
            case DASHBOARD -> evict("nursing:dashboard:v1");
            case STUDENT_DEPT_DISTRIBUTION -> evictPattern("nursing:admin:settings:student_department_distribution:v1:*");
            case TAG_OVERVIEW -> evict("nursing:tag:overview");
            case TAG_STATISTICS -> evict("nursing:tag:statistics");
            case CATEGORY_TREE -> evictPattern("nursing:category:tree:v1:*");
            case CATEGORY_OVERVIEW -> evict("nursing:category:overview:v1");
            case ALL -> {
                evict("nursing:dashboard:v1");
                evictPattern("nursing:category:tree:v1:*");
                evict("nursing:category:overview:v1");

                evictPattern("nursing:admin:settings:student_department_distribution:v1:*");
                evict("nursing:tag:overview");
                evict("nursing:tag:statistics");
            }
        }
    }

    private void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("已清除缓存: {}", key);
        } catch (Exception e) {
            log.warn("清除缓存失败: {}", key, e);
        }
    }

    private void evictPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.debug("已清除缓存, pattern={}", pattern);
        } catch (Exception e) {
            log.warn("清除缓存失败, pattern={}", pattern, e);
        }
    }
}

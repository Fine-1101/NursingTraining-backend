
package org.example.nursingtrainingbackend.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.nursingtrainingbackend.common.annotation.DistributedLock;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = parseSpel(distributedLock.key(), joinPoint);
        String lockKey = "nursing:lock:" + key;

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁
            boolean acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("获取分布式锁失败，任务已被其他实例执行: {}", lockKey);
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }

            log.debug("获取分布式锁成功: {}", lockKey);
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁: {}", lockKey);
            }
        }
    }

    private String parseSpel(String spel, ProceedingJoinPoint joinPoint) {
        Expression expression = parser.parseExpression(spel);
        EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withInstanceMethods()
                .build();
        context.setVariable("args", joinPoint.getArgs());
        context.setVariable("method", joinPoint.getSignature().getName());

        Object value = expression.getValue(context, joinPoint.getThis(), Object.class);
        return value != null ? value.toString() : spel;
    }
}

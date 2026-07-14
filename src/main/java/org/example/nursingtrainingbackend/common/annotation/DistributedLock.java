
package org.example.nursingtrainingbackend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 锁的键，支持 SpEL 表达式
     */
    String key();

    /**
     * 锁的超时时间（秒）
     */
    long leaseTime() default 60;

    /**
     * 等待锁的超时时间（秒）
     */
    long waitTime() default 10;
}


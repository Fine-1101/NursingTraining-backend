
package org.example.nursingtrainingbackend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流标识
     */
    String key() default "";

    /**
     * 限流时间，单位秒
     */
    int time() default 60;

    /**
     * 限流次数
     */
    int count() default 10;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 默认策略全局限流
         */
        DEFAULT,
        /**
         * 根据请求者IP进行限流
         */
        IP,
        /**
         * 根据用户进行限流
         */
        USER
    }
}

package org.example.nursingtrainingbackend.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 仅当 app.oss.access-key-id 配置了非空值时，才创建 OSS 相关 Bean
 */
public class OssEnabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String key = context.getEnvironment().getProperty("app.oss.access-key-id");
        return key != null && !key.isBlank();
    }
}

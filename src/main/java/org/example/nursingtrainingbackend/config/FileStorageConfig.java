package org.example.nursingtrainingbackend.config;

import org.example.nursingtrainingbackend.config.properties.UploadProperties;
import org.example.nursingtrainingbackend.modules.file.service.FileService;
import org.example.nursingtrainingbackend.modules.file.service.impl.LocalFileServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 文件存储配置 — 根据 OSS 是否启用选择实现
 */
@Configuration(proxyBeanMethods = false)
public class FileStorageConfig {

    /**
     * 当 OSS 未配置时（access-key-id 为空或不存在），使用本地文件存储
     */
    @Bean
    @Conditional(OssDisabledCondition.class)
    public FileService localFileService(UploadProperties uploadProperties, Environment env) {
        return new LocalFileServiceImpl(uploadProperties, env);
    }

    /**
     * OSS 未启用的条件 — 与 OssEnabledCondition 相反
     */
    static class OssDisabledCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String key = context.getEnvironment().getProperty("app.oss.access-key-id");
            return key == null || key.isBlank();
        }
    }
}

package org.example.nursingtrainingbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vod")
public record VodProperties(String accessKeyId, String accessKeySecret,
                            String regionId, String transcodeTemplateGroupId) {
    public boolean configured() {
        return notBlank(accessKeyId) && notBlank(accessKeySecret) && notBlank(regionId);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
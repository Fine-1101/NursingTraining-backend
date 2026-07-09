package org.example.nursingtrainingbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.oss")
public record OssProperties(String endpoint, String accessKeyId, String accessKeySecret,
                            String bucketName, String publicDomain, String baseDirectory,
                            Duration policyExpiration) {
    public boolean configured() {
        return notBlank(endpoint) && notBlank(accessKeyId) && notBlank(accessKeySecret) && notBlank(bucketName);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

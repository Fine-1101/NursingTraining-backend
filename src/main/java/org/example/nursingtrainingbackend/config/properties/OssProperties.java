package org.example.nursingtrainingbackend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String publicDomain;
    private String baseDirectory;
    private Duration policyExpiration;

    public boolean configured() {
        return notBlank(endpoint) && notBlank(accessKeyId) &&
                notBlank(accessKeySecret) && notBlank(bucketName);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
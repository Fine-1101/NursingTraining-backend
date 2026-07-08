package org.example.nursingtrainingbackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Set;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(long maxSize, Set<String> allowedContentTypes) {}

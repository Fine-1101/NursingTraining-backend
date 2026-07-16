package org.example.nursingtrainingbackend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.ppt-conversion")
public class PptConversionProperties {

    private boolean enabled = true;
    private String libreOfficePath = "soffice";
    private Duration timeout = Duration.ofMinutes(2);
    private String previewDirectory = "nursing-training/ppts/previews";
}

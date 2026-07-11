package org.example.nursingtrainingbackend.config;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.config.properties.CorsProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final CorsProperties corsProperties;

    @Value("${app.upload.local-directory:./uploads}")
    private String localDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        // 本地文件上传目录映射
        String absolutePath = java.nio.file.Paths.get(localDirectory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(absolutePath);
    }
}

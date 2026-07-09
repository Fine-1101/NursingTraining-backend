package org.example.nursingtrainingbackend.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.Version;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ext.javatime.JavaTimeInitializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer javaTimeMapperCustomizer() {
        return new JsonMapperBuilderCustomizer() {
            @Override
            public void customize(JsonMapper.Builder builder) {
                builder.addModule(new JacksonModule() {
                    @Override
                    public String getModuleName() {
                        return "JavaTimeWrapperModule";
                    }

                    @Override
                    public Version version() {
                        return Version.unknownVersion();
                    }

                    @Override
                    public void setupModule(SetupContext context) {
                        JavaTimeInitializer.getInstance().setupModule(context);
                    }
                });
            }
        };
    }
}

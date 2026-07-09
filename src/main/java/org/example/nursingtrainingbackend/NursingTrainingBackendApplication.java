package org.example.nursingtrainingbackend;

import org.example.nursingtrainingbackend.config.properties.OssProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(OssProperties.class)
@MapperScan({
        "org.example.nursingtrainingbackend.modules.ppt.mapper",
        "org.example.nursingtrainingbackend.modules.user.mapper",
        // 其他 mapper 包
})
public class NursingTrainingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NursingTrainingBackendApplication.class, args);
    }

}

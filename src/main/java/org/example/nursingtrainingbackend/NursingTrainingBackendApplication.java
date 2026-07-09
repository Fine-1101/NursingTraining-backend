package org.example.nursingtrainingbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan({"org.example.nursingtrainingbackend.modules.user.mapper", "org.example.nursingtrainingbackend.modules.tag.mapper"})
public class NursingTrainingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NursingTrainingBackendApplication.class, args);
    }

}

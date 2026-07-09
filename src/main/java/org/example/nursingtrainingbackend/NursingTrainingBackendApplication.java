package org.example.nursingtrainingbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@MapperScan("org.example.nursingtrainingbackend.modules.**.mapper")
public class NursingTrainingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NursingTrainingBackendApplication.class, args);
    }

}

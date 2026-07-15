package org.example.nursingtrainingbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class LearningReportAsyncConfig {

    @Bean("learningReportExecutor")
    public Executor learningReportExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix(
                "learning-report-"
        );
        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }
}
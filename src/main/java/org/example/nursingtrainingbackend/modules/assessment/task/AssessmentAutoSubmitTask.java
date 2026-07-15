package org.example.nursingtrainingbackend.modules.assessment.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.modules.assessment.service.LearnerAssessmentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssessmentAutoSubmitTask {

    private final LearnerAssessmentService learnerAssessmentService;

    @Scheduled(fixedDelayString = "${app.assessment.auto-submit-interval:10s}")
    public void autoSubmitExpiredAttempts() {
        try {
            int count = learnerAssessmentService.autoSubmitExpiredAttempts();
            if (count > 0) {
                log.info("考核定时自动收卷完成，count={}", count);
            }
        } catch (Exception exception) {
            log.error("考核定时自动收卷失败", exception);
        }
    }
}

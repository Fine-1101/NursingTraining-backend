package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员端 — 课程考核卡片
 */
public record AssessmentCardVO(
        Long assessmentId,
        String title,
        String description,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTime,

        Integer durationMinutes,
        BigDecimal totalScore,
        BigDecimal passScore,
        Integer maxAttempts,
        Integer usedAttempts,
        Integer remainingAttempts,

        /** NOT_OPEN / NOT_STARTED / IN_PROGRESS / PASSED / FAILED / CLOSED / NO_ATTEMPTS */
        String state,

        Long currentAttemptId,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime currentDeadlineAt,

        Long latestAttemptId,
        BigDecimal latestScore,
        Boolean passed,

        /** NONE / START / CONTINUE / VIEW_RESULT / RETRY */
        String action,
        Boolean actionEnabled,
        String disabledReason
) {
}

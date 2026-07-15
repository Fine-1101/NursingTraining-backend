package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员端 — 交卷响应
 */
public record SubmitAttemptVO(
        Long attemptId,
        Long assessmentId,
        BigDecimal score,
        BigDecimal totalScore,
        BigDecimal passScore,
        Boolean passed,
        Integer correctCount,
        Integer wrongCount,
        Integer unansweredCount,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startedAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime submittedAt,

        Integer status,
        Integer remainingAttempts
) {
}

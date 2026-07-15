package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员端 — 成绩详情（第一版只返回成绩和统计，不返回试题正确答案与解析）
 */
public record AttemptResultVO(
        Long attemptId,
        Long assessmentId,
        String assessmentTitle,
        Long courseId,
        String courseTitle,
        Integer attemptNo,
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

        Long durationSeconds,
        Integer status,
        Integer remainingAttempts
) {
}

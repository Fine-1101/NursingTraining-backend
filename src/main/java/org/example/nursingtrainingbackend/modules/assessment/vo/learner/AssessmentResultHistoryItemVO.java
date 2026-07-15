package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssessmentResultHistoryItemVO(
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
        Integer status,
        String statusName,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startedAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime submittedAt,

        Long durationSeconds
) {
}

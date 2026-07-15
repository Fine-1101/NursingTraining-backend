package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 学员端 — 保存单题答案响应
 */
public record SaveAnswerResponse(
        Long attemptQuestionId,
        String selectedOptionKey,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime savedAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTime,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime deadlineAt,

        Long remainingSeconds
) {
}

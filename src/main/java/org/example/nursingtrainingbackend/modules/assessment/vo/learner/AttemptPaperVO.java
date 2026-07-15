package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员端 — 试卷详情
 */
public record AttemptPaperVO(
        Long attemptId,
        Long assessmentId,
        String assessmentTitle,
        Integer attemptNo,
        Integer status,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startedAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime deadlineAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTime,

        BigDecimal totalScore,
        Integer answeredCount,
        Integer totalQuestionCount,

        List<PaperQuestionVO> questions
) {

    public record PaperQuestionVO(
            Long attemptQuestionId,
            Integer questionType,
            String stem,
            BigDecimal score,
            Integer number,
            List<PaperOptionVO> options,
            String selectedOptionKey,

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime savedAt
    ) {
    }

    public record PaperOptionVO(
            String optionKey,
            String content
    ) {
    }
}

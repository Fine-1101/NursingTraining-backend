package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AttemptReviewVO(
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
        LocalDateTime submittedAt,

        List<QuestionReviewVO> questions
) {
    public record QuestionReviewVO(
            Long attemptQuestionId,
            Integer number,
            Integer questionType,
            String stem,
            BigDecimal maxScore,
            BigDecimal earnedScore,
            String selectedOptionKey,
            String correctOptionKey,
            Boolean correct,
            String analysis,
            List<OptionReviewVO> options
    ) {
    }

    public record OptionReviewVO(
            String optionKey,
            String content,
            Boolean selected,
            Boolean correct
    ) {
    }
}

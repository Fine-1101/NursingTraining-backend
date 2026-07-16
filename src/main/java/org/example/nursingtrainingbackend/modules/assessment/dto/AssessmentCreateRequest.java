package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建考核草稿请求
 */
public record AssessmentCreateRequest(
        @NotNull Long courseId,
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startAt,
        LocalDateTime endAt,
        @NotNull Integer durationMinutes,
        @NotNull BigDecimal passScore,
        @NotNull Integer maxAttempts,
        @NotNull Integer difficultyLevel,
        @NotNull List<DrawRuleItem> drawRules
) {
    public record DrawRuleItem(
            @NotNull Integer questionType,
            Integer difficulty,
            @NotNull Integer questionCount,
            @NotNull BigDecimal scorePerQuestion
    ) {
    }
}

package org.example.nursingtrainingbackend.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考核详情（含抽题规则）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentDetailVO {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer durationMinutes;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private Integer difficultyLevel;
    private Integer status;

    private List<DrawRuleVO> drawRules;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrawRuleVO {
        private Integer questionType;
        private Integer difficulty;
        private Integer questionCount;
        private BigDecimal scorePerQuestion;
        /** 可用题目数 */
        private Long availableCount;
        /** 题量是否充足 */
        private Boolean sufficient;
    }
}

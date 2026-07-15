package org.example.nursingtrainingbackend.modules.assessment.vo;

import java.math.BigDecimal;

/**
 * 创建考核草稿响应VO
 */
public record AssessmentCreateResponseVO(
        Long id,
        Long categoryId,
        BigDecimal totalScore,
        Integer status
) {
}

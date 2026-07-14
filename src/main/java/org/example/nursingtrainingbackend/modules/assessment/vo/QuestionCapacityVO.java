package org.example.nursingtrainingbackend.modules.assessment.vo;

import java.util.List;

/**
 * 题量检查结果
 */
public record QuestionCapacityVO(
        Long assessmentId,
        Long courseId,
        Long categoryId,
        List<ItemVO> items,
        Boolean publishable
) {

    public record ItemVO(
            Integer questionType,
            Integer requiredCount,
            Long availableCount,
            Boolean sufficient
    ) {
    }
}

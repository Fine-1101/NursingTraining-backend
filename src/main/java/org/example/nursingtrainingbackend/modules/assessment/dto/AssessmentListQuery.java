package org.example.nursingtrainingbackend.modules.assessment.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 考核列表查询参数
 */
public record AssessmentListQuery(
        String keyword,
        Long courseId,
        Long categoryId,
        Integer status,
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startFrom,
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTo,
        Long page,
        Long size
) {
}

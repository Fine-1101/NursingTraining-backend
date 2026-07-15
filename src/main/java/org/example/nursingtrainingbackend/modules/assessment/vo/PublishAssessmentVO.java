package org.example.nursingtrainingbackend.modules.assessment.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 发布考核响应VO
 */
public record PublishAssessmentVO(
        Long id,
        Integer status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime publishedAt
) {
}

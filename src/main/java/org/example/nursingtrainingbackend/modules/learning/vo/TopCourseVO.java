package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TopCourseVO {

    private Integer rank;

    private Long courseId;

    private String courseTitle;

    private Integer recordCount;

    private Integer totalDurationMinutes;

    private BigDecimal totalDurationHours;

    private LocalDateTime lastLearnedAt;

    private BigDecimal barPercent;
}

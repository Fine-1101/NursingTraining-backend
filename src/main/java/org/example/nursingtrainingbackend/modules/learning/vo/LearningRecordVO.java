package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LearningRecordVO {

    private Long recordId;

    private String actionType;

    private String actionName;

    private Long courseId;

    private String courseTitle;

    private Long coursePointId;

    private String coursePointTitle;

    private String resourceType;

    private String resourceTypeName;

    private Long resourceId;

    private String resourceTitle;

    private String title;

    private String description;

    private Integer durationMinutes;

    private BigDecimal progressPercent;

    private LocalDateTime occurredAt;

    private String timeText;
}

package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class CreateCourseInitialVO {

    private Long courseId;

    private String status = "DRAFT";

    private String completionRule = "ALL_REQUIRED_POINTS";

    private Integer currentStep = 2;

    private String createdAt;

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}

package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class CourseUpdateBasicVO {

    private Long courseId;

    private String status;

    private Integer currentStep;

    private String updatedAt;

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        if (updatedAt != null) {
            this.updatedAt = updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}

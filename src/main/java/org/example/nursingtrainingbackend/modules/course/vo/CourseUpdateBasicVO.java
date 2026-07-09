package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseUpdateBasicVO {

    private Long courseId;

    private String status;

    private Integer currentStep;

    private LocalDateTime updatedAt;
}
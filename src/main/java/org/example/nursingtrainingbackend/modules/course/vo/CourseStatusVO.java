package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseStatusVO {

    private Long courseId;

    private String status;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}

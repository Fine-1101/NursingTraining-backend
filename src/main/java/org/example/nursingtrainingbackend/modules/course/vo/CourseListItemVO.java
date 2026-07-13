package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseListItemVO {
    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private String status;
    private String instructorName;
    private String categoryName;
    private Integer studentCount;
    private Integer chapterCount;
    private Integer pointCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}

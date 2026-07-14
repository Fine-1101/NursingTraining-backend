package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateChapterVO {
    private Long id;
    private String title;
    private Integer sort;
    private LocalDateTime updatedAt;
}

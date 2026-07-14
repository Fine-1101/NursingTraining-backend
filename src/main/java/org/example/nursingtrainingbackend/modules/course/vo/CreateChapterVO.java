package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

@Data
public class CreateChapterVO {
    private Long id;
    private Long courseId;
    private String title;
    private Integer sort;
    private Integer pointCount=0;
}

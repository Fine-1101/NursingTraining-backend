package org.example.nursingtrainingbackend.modules.system.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseProgressItemVO {

    private Long courseId;
    private String courseTitle;
    private String courseType;
    private String learningStatus;
    private BigDecimal progressPercent;
}

package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCourseInitialVO {
    /**
     * 新课程ID（数据库自增主键）
     */
    private Long courseId;

    /**
     * 课程状态，固定 DRAFT
     */
    private String status="DRAFT";

    /**
     * 完成规则，固定 ALL_REQUIRED_POINTS
     */
    private String completionRule="ALL_REQUIRED_POINTS";

    /**
     * 当前步骤，固定 2
     */
    private Integer currentStep=2;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

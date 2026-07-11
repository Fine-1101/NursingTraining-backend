package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员课程列表项VO
 */
@Data
public class LearnerCourseVO {

    /** 课程ID */
    private Long courseId;

    /** 课程名称 */
    private String title;

    /** 课程简介 */
    private String summary;

    /** 课程封面URL */
    private String coverUrl;

    /** 讲师姓名 */
    private String instructorName;

    /** 课程类别名称 */
    private String categoryName;

    /** 课程类型：REQUIRED-必修，OPTIONAL-选修 */
    private String courseType;

    /** 学习状态：NOT_STARTED、LEARNING、COMPLETED */
    private String learningStatus;

    /** 当前进度百分比 */
    private BigDecimal progressPercent;

    /** 已完成课程点数量 */
    private Integer completedPointCount;

    /** 课程总课程点数量 */
    private Integer pointCount;

    /** 最近学习时间 */
    private LocalDateTime lastLearnedAt;

    /** 按钮文案 */
    private String buttonText;
}

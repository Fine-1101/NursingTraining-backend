package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推荐课程VO
 */
@Data
public class RecommendedCourseVO {

    /** 课程ID */
    private Long courseId;

    /** 课程名称 */
    private String title;

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

    /** 当前学员该课程进度 */
    private BigDecimal progressPercent;

    /** 按钮文案：开始学习、继续学习、复习课程 */
    private String buttonText;

    /** 最近学习课程点ID；未开始时可返回第一个课程点 */
    private Long lastPointId;
}

package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 继续学习课程VO
 */
@Data
public class ContinueCourseVO {

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

    /** 学习状态：固定为LEARNING */
    private String learningStatus;

    /** 当前课程进度 */
    private BigDecimal progressPercent;

    /** 按钮文案：固定为继续学习 */
    private String buttonText;

    /** 最近学习课程点ID */
    private Long lastPointId;

    /** 最近学习课程点名称 */
    private String lastPointTitle;

    /** 最近学习时间 */
    private LocalDateTime lastLearnedAt;
}

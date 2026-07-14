package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

/**
 * 开始学习响应VO
 */
@Data
public class StartLearningVO {

    /** 课程ID */
    private Long courseId;

    /** 课程名称 */
    private String title;

    /** 学习状态：NOT_STARTED、LEARNING、COMPLETED */
    private String learningStatus;

    /** 当前学习课程点ID（用于前端跳转） */
    private Long currentPointId;

    /** 当前学习课程点标题 */
    private String currentPointTitle;

    /** 按钮文案 */
    private String buttonText;
}

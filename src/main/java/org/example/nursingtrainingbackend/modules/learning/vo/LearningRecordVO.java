package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录VO
 */
@Data
public class LearningRecordVO {

    /** 学习记录类型：COURSE_LEARNING、RESOURCE_COMPLETED、POINT_COMPLETED、COURSE_COMPLETED */
    private String recordType;

    /** 展示标题 */
    private String title;

    /** 课程ID */
    private Long courseId;

    /** 课程名称 */
    private String courseTitle;

    /** 课程点ID */
    private Long coursePointId;

    /** 课程点名称 */
    private String coursePointTitle;

    /** 课件类型：ARTICLE、VIDEO、PPT；课程级记录为空 */
    private String resourceType;

    /** 课件ID；课程级记录为空 */
    private Long resourceId;

    /** 课件名称 */
    private String resourceTitle;

    /** 记录发生时间 */
    private LocalDateTime occurredAt;
}

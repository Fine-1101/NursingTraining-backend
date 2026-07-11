package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

/**
 * 课程状态统计VO
 */
@Data
public class CourseStatsVO {

    /** 当前学员可学习课程总数 */
    private Integer allCount;

    /** 符合推荐规则的课程数量 */
    private Integer recommendedCount;

    /** 当前学员所在部门必修课程数量 */
    private Integer requiredCount;

    /** 当前学员所在部门选修课程数量 */
    private Integer optionalCount;

    /** 已完成课程数量 */
    private Integer completedCount;

    /** 学习中课程数量 */
    private Integer learningCount;

    /** 未开始课程数量 */
    private Integer notStartedCount;
}

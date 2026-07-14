package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 学习进度概览VO
 */
@Data
public class ProgressOverviewVO {

    /** 学员可学习课程总数 */
    private Integer totalCount;

    /** 已完成课程数量 */
    private Integer completedCount;

    /** 学习中课程数量 */
    private Integer learningCount;

    /** 未开始课程数量 */
    private Integer notStartedCount;

    /** 总体课程完成率，completedCount / totalCount * 100 */
    private BigDecimal overallProgressPercent;
}

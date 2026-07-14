package org.example.nursingtrainingbackend.modules.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 课程学习趋势查询结果行 */
@Data
public class CourseTrendRow {
    private String periodStart;
    private String label;
    private Integer learnerCount;
    private Integer completedLearnerCount;
    private BigDecimal completionRate;
}

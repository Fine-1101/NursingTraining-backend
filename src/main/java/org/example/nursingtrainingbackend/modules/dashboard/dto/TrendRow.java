package org.example.nursingtrainingbackend.modules.dashboard.dto;

import lombok.Data;

/** 学习趋势查询结果行 */
@Data
public class TrendRow {
    private String label;
    private String period;
    private Integer learnerCount;
    private Integer completedCourseCount;
}

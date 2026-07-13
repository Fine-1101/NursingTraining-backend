package org.example.nursingtrainingbackend.modules.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 课程学习趋势查询结果行 */
@Data
public class CourseTrendRow {
    private String label;
    private Integer learnerCount;
    private BigDecimal completionRate;
}

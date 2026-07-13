package org.example.nursingtrainingbackend.modules.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 科室排行查询结果行 */
@Data
public class DepartmentRankRow {
    private Long departmentId;
    private String departmentName;
    private Integer learnerCount;
    private Integer completedLearnerCount;
    private BigDecimal completionRate;
}

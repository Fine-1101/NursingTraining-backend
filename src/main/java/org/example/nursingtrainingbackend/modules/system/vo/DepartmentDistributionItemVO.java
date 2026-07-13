package org.example.nursingtrainingbackend.modules.system.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepartmentDistributionItemVO {

    private Long departmentId;
    private String departmentName;
    private Integer studentCount;
    private BigDecimal percent;
}

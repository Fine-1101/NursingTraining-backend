package org.example.nursingtrainingbackend.modules.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CourseProgressVO {

    private Long studentId;
    private String realName;
    private BigDecimal averageProgressPercent;
    private Integer courseCount;
    private List<CourseProgressItemVO> items;
}

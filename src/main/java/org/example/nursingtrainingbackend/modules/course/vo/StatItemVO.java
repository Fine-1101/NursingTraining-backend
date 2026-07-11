package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatItemVO {
    /**
     * 当前实时值
     */
    private Long value;

    /**
     * 上月月末快照值，无快照时为 null
     */
    private Long previousValue;

    /**
     * 变化百分比，无法计算时为 null
     */
    private BigDecimal changeRate;

    /**
     * UP、DOWN、SAME 或 NO_DATA
     */
    private String changeDirection;
}

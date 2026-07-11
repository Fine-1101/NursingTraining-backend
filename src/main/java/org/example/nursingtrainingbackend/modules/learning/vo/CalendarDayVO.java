package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 日历日期VO
 */
@Data
public class CalendarDayVO {

    /** 日期 */
    private LocalDate date;

    /** 日期中的日 */
    private Integer dayOfMonth;

    /** 是否属于当前月份 */
    private Boolean currentMonth;

    /** 是否今天 */
    private Boolean today;

    /** 当天是否有学习行为 */
    private Boolean hasLearning;
}

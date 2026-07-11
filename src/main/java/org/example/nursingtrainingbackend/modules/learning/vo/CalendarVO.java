package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习日历VO
 */
@Data
public class CalendarVO {

    /** 日历年份 */
    private Integer year;

    /** 日历月份，1~12 */
    private Integer month;

    /** 当前日期 */
    private LocalDate today;

    /** 日历日期列表 */
    private List<CalendarDayVO> days;
}

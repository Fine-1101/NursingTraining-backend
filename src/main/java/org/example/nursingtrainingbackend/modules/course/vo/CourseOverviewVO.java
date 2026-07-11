package org.example.nursingtrainingbackend.modules.course.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CourseOverviewVO {
    /**
     * 课程总数统计
     */
    private StatItemVO total;

    /**
     * 草稿课程统计
     */
    private StatItemVO draft;

    /**
     * 已发布课程统计
     */
    private StatItemVO published;

    /**
     * 已下架课程统计
     */
    private StatItemVO offline;

    /**
     * 实际使用的比较快照日期（上月月末），无快照时为 null
     */
    private LocalDate comparisonDate;
}

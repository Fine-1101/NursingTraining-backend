package org.example.nursingtrainingbackend.modules.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习数据趋势下钻响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningTrendDrillVO {

    /** 当前层级: MONTH / WEEK / DAY */
    private String level;

    /** 面包屑导航 */
    private List<BreadcrumbItem> breadcrumbs;

    /** 趋势数据点 */
    private List<DrillTrendPoint> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreadcrumbItem {
        /** 标签文字，如 "2026年7月"、"第3周" */
        private String label;
        /** 层级: MONTH / WEEK */
        private String level;
        /** 年份 */
        private Integer year;
        /** 月份 (1-12) */
        private Integer month;
        /** 周序号 (1-5)，仅 WEEK 层级有值 */
        private Integer weekIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrillTrendPoint {
        /** 显示标签，如 "第1周"、"07-10" */
        private String label;
        /** 周期标识，如 "2026-W28"、"2026-07-10" */
        private String period;
        /** 学习人数 */
        private Integer learnerCount;
        /** 课程完成数 */
        private Integer completedCourseCount;
    }
}

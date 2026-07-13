package org.example.nursingtrainingbackend.modules.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    private SummaryCards summaryCards;

    private LearningStatusDistribution learningStatusDistribution;

    private LearningTrend learningTrend;

    private CourseLearningTrend courseLearningTrend;

    private List<DepartmentRankingItem> departmentCompletionRanking;

    private List<QuickEntry> quickEntries;

    // ========== 顶部统计卡片 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryCards {
        private CardItem courseTotal;
        private CardItem learnerTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardItem {
        private Integer value;
        private BigDecimal changeRate;
        private String changeDirection; // UP, DOWN, SAME, NO_DATA
    }

    // ========== 学习状态分布 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningStatusDistribution {
        private Integer totalLearners;
        private List<StatusItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusItem {
        private String status;
        private String statusName;
        private Integer count;
        private BigDecimal percent;
    }

    // ========== 学习数据趋势 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningTrend {
        private String range;
        private String unit; // WEEK or MONTH
        private List<TrendPoint> points;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private String period;
        private Integer learnerCount;
        private Integer completedCourseCount;
    }

    // ========== 课程学习趋势 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseLearningTrend {
        private String courseTitle;
        private List<CourseTrendPoint> points;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseTrendPoint {
        private String label;
        private Integer learnerCount;
        private BigDecimal completionRate;
    }

    // ========== 科室排行 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentRankingItem {
        private Long departmentId;
        private String departmentName;
        private Integer learnerCount;
        private Integer completedLearnerCount;
        private BigDecimal completionRate;
    }

    // ========== 快捷入口 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickEntry {
        private String code;
        private String title;
        private String path;
    }
}

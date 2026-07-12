package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecordStatsVO {

    private Integer totalRecords;

    private Integer courseLearningCount;

    private Integer courseCompletedCount;

    private Integer pointCompletedCount;

    private Integer resourceCompletedCount;

    private BigDecimal overallProgressPercent;

    private Integer totalLearningDays;

    private Integer consecutiveDays;

    private List<ResourceDistributionVO> resourceDistribution;

    private FrequencyTrendVO frequencyTrend;

    private List<TopCourseVO> topCourses;
}
